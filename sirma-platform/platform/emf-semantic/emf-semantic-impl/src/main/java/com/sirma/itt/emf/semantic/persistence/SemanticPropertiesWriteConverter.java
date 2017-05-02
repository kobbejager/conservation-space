package com.sirma.itt.emf.semantic.persistence;

import static com.sirma.itt.seip.domain.definition.PropertyDefinition.resolveUri;
import static com.sirma.itt.seip.util.EqualsHelper.getMapComparison;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.emf.semantic.persistence.PersistStep.PersistStepFactory;
import com.sirma.itt.seip.configuration.Options;
import com.sirma.itt.seip.db.DatabaseIdManager;
import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.definition.SemanticDefinitionService;
import com.sirma.itt.seip.domain.DmsAware;
import com.sirma.itt.seip.domain.Purposable;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.PropertyDefinition;
import com.sirma.itt.seip.domain.instance.ClassInstance;
import com.sirma.itt.seip.domain.instance.DefaultProperties;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.OwnedModel;
import com.sirma.itt.seip.event.EventService;
import com.sirma.itt.seip.instance.properties.SemanticNonPersistentPropertiesExtension;
import com.sirma.itt.seip.monitor.Statistics;
import com.sirma.itt.seip.plugin.ExtensionPoint;
import com.sirma.itt.seip.time.TimeTracker;
import com.sirma.itt.seip.util.EqualsHelper.MapValueComparison;
import com.sirma.itt.semantic.NamespaceRegistryService;
import com.sirma.itt.semantic.model.vocabulary.EMF;
import com.sirma.itt.semantic.model.vocabulary.Proton;

/**
 * Utility class for building semantic persistent model used to store instance changes to database.
 *
 * @author BBonev
 */
@ApplicationScoped
public class SemanticPropertiesWriteConverter extends BasePropertiesConverter {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SemanticPropertiesWriteConverter.class);
	private Set<String> forbiddenProperties;

	@Inject
	private DictionaryService dictionaryService;

	@Inject
	@ExtensionPoint(value = SemanticNonPersistentPropertiesExtension.TARGET_NAME)
	private Iterable<SemanticNonPersistentPropertiesExtension> nonPersistentProperties;

	@Inject
	private DatabaseIdManager idManager;

	@Inject
	private SemanticDefinitionService semanticDefinitionService;

	@Inject
	private EventService eventService;

	@Inject
	private Statistics statistics;

	@Inject
	private PersistStepFactoryBuilder persistStepFactoryBuilder;

	/**
	 * Initializes instance variables.
	 */
	@PostConstruct
	public void postConstruct() {
		forbiddenProperties = new LinkedHashSet<>(50);
		for (SemanticNonPersistentPropertiesExtension extension : nonPersistentProperties) {
			forbiddenProperties.addAll(extension.getNonPersistentProperties());
		}
	}

	/**
	 * Builds add and remove models from the new and old instances. The method creates diffs based on the given
	 * instances and updates respected models with statements needed to be executed in order to save the changes
	 * detected in the instances. If the old instance is <code>null</code> the method will build the model in such way
	 * only to add the new instance without duplicates in semantic database.
	 * <p>
	 * NOTE: For more accurate and optimal persist of an instance the old instance is strongly recommended.
	 *
	 * @param newInstance
	 *            the new instance
	 * @param oldInstance
	 *            the old instance if any (optional)
	 * @param addModel
	 *            the model that will be added to database
	 * @param removeModel
	 *            the model to will be removed from database
	 * @return the target instance URI as resource
	 */
	@SuppressWarnings("boxing")
	public Resource buildModelForInstance(Instance newInstance, Instance oldInstance, Model addModel,
			Model removeModel) {

		TimeTracker tracker = statistics.createTimeStatistics(getClass(), "buildPersistModel");
		// remove properties that should not be persisted
		removeForbiddenProperties(newInstance, oldInstance);

		// if the passed instance is newly created we should generate its identifier
		idManager.generateStringId(newInstance, true);

		// set the subject's type
		URI subject = namespaceRegistryService.buildUri(newInstance.getId().toString());

		PersistStepFactory stepFactory = persistStepFactoryBuilder.build(subject, newInstance, oldInstance);

		// add instance's member variables diff
		Stream<PersistStep> memberVariables = getInstanceMemberVariables(stepFactory, newInstance, oldInstance);
		// add instance properties diff
		Stream<PersistStep> properties = getPropertiesPersistSteps(stepFactory, newInstance, oldInstance);

		Stream
				.concat(memberVariables, properties)
					.flatMap(PersistStep::getStatements)
					.filter(LocalStatement::hasStatement)
					.flatMap(notifyForObjectPropertyChange())
					.forEach(localStatement -> localStatement.addTo(addModel, removeModel));

		LOGGER.trace("Persist model build for {} took {} ms", newInstance.getId(), tracker.stop());
		return subject;
	}

	/**
	 * Gets a stream of persist steps generated for the given instance versions. The returned steps if any should
	 * generate statements for add and remove to the database
	 *
	 * @param stepFactory
	 *            the step factory
	 * @param newData
	 *            the new instance data that should be persisted
	 * @param oldData
	 *            the old instance data. It's used for generating delete statements. It should be the same data that is
	 *            currently in the database
	 * @return the stream of properties steps
	 */
	private Stream<PersistStep> getPropertiesPersistSteps(PersistStepFactory stepFactory, Instance newData,
			Instance oldData) {

		DefinitionModel definition = dictionaryService.getInstanceDefinition(newData);
		// the first instance will be null on delete, so we will use the second
		if (definition == null && oldData != null) {
			definition = dictionaryService.getInstanceDefinition(oldData);
		}

		Stream<PersistStep> definedProperties = Stream.empty();
		if (definition != null) {
			// all properties that are defined in the definition
			definedProperties = definition.fieldsStream().map(stepFactory::create);
		}

		// all other properties that are not defined in the definition if any
		// we will process only properties that have URIs as names as we do not have any other way to define a mapping
		// for non URI format compliant properties and URI format
		Stream<PersistStep> nonDefinedProperties = getPropertiesDiff(newData, oldData)
				.entrySet()
					.stream()
					.filter(entriesWithChanges())
					.map(Entry::getKey)
					.filter(validUris(definition))
					.map(stepFactory::create);

		return Stream.concat(definedProperties, nonDefinedProperties);
	}

	private static Predicate<Entry<String, MapValueComparison>> entriesWithChanges() {
		return entry -> entry.getValue() != MapValueComparison.EQUAL;
	}

	private static Predicate<String> validUris(DefinitionModel definition) {
		Set<String> definedFields = getDefinitionProperties(definition);
		return name -> name.contains(NamespaceRegistryService.SHORT_URI_DELIMITER) && !definedFields.contains(name);
	}

	/**
	 * Collect all unique field names and URIs
	 *
	 * @param definitionModel
	 *            to fetch the fields from
	 * @return the collected field names and URIs or empty set for <code>null</code> definition
	 */
	private static Set<String> getDefinitionProperties(DefinitionModel definitionModel) {
		if (definitionModel == null) {
			return Collections.emptySet();
		}

		return definitionModel
				.fieldsStream()
					.filter(PropertyDefinition.hasUri())
					.flatMap(field -> Stream.of(field.getName(), resolveUri().apply(field)))
					.collect(Collectors.toSet());
	}

	private Function<LocalStatement, Stream<LocalStatement>> notifyForObjectPropertyChange() {
		EventService service = eventService;
		NamespaceRegistryService registryService = namespaceRegistryService;

		// this second pass does not trigger events to trigger infinite event firing
		Function<LocalStatement, Stream<LocalStatement>> secondPass = statement -> {
			if (isObjectPropertyStatement(statement)) {
				BaseRelationChangeEvent event = fireObjectPropertyEvent(statement, service, registryService);
				return Stream.concat(Stream.of(statement), event.getStatements());
			}
			return Stream.of(statement);
		};

		return statement -> {
			if (isObjectPropertyStatement(statement)) {
				BaseRelationChangeEvent event = fireObjectPropertyEvent(statement, service, registryService);
				// return the current statement and any other statements if any
				// scan the returned statements for relation properties and fire events for them also
				return Stream.concat(Stream.of(statement), event.getStatements().flatMap(secondPass));
			}
			return Stream.of(statement);
		};
	}

	private static BaseRelationChangeEvent fireObjectPropertyEvent(LocalStatement statement, EventService service,
			NamespaceRegistryService registryService) {
		BaseRelationChangeEvent event;
		if (statement.isToAdd()) {
			event = fireAddRelationEvent(statement, service, registryService);
		} else {
			event = fireRemoveRelationEvent(statement, service, registryService);
		}
		return event;
	}

	private static AddRelationEvent fireAddRelationEvent(LocalStatement statement, EventService service,
			NamespaceRegistryService registryService) {
		AddRelationEvent event = new AddRelationEvent(statement, registryService::getShortUri);
		service.fire(event);
		return event;
	}

	private static RemoveRelationEvent fireRemoveRelationEvent(LocalStatement statement, EventService service,
			NamespaceRegistryService registryService) {
		RemoveRelationEvent event = new RemoveRelationEvent(statement, registryService::getShortUri);
		service.fire(event);
		return event;
	}

	private static boolean isObjectPropertyStatement(LocalStatement statement) {
		return statement.getStatement().getObject() instanceof URI;
	}

	private static Map<String, MapValueComparison> getPropertiesDiff(Instance newData, Instance oldData) {
		if (oldData == null) {
			return getMapComparison(newData.getProperties(), Collections.<String, Serializable> emptyMap());
		}
		return getMapComparison(newData.getProperties(), oldData.getProperties());
	}

	/**
	 * Removes the forbidden properties.
	 *
	 * @param newInstance
	 *            the new instance
	 * @param oldInstance
	 *            the old instance
	 */
	private void removeForbiddenProperties(Instance newInstance, Instance oldInstance) {
		newInstance.removeProperties(forbiddenProperties);
		if (oldInstance != null) {
			oldInstance.removeProperties(forbiddenProperties);
		}
	}

	/**
	 * Gets the instance {@link URI}.
	 *
	 * @param instance
	 *            the instance
	 * @return the instance {@link URI}
	 */
	public URI getInstanceRdfType(Instance instance) {
		Serializable propertyValue = instance.get(DefaultProperties.SEMANTIC_TYPE,
				() -> instance.get(RDF.TYPE.toString(), () -> instance.type().getId()));
		if (propertyValue instanceof URI) {
			return (URI) propertyValue;
		}
		if (propertyValue != null) {
			return namespaceRegistryService.buildUri(propertyValue.toString());
		}
		if (instance.type() != null) {
			return namespaceRegistryService.buildUri(instance.type().getId().toString());
		}
		throw new IllegalArgumentException("Uknown instance type [" + instance + "]");
	}

	/**
	 * Add instance's member variables and owning reference to model if applicable and present.
	 *
	 * @param subject
	 *            the subject
	 * @param newEntity
	 *            the new entity
	 * @param oldEntity
	 *            the old entity
	 * @param inverseRelationProvider
	 *            the inverse relation provider
	 * @return the builder
	 */
	private Stream<PersistStep> getInstanceMemberVariables(PersistStepFactory stepFactory, Instance newEntity,
			Instance oldEntity) {

		Builder<PersistStep> instanceVariables = Stream.builder();

		if (oldEntity == null) {
			URI instanceUri = getInstanceRdfType(newEntity);
			instanceVariables.add(stepFactory.create(RDF.TYPE, instanceUri, null));

			ClassInstance classInstance = semanticDefinitionService.getClassInstance(instanceUri.toString());
			// this type is used later for instantiating the instance into java object
			String instanceTyoe = classInstance.getProperty(EMF.DEFINITION_ID.getLocalName());
			instanceVariables.add(stepFactory.create(EMF.INSTANCE_TYPE, instanceTyoe, null));
		}

		instanceVariables.add(getDefinitionId(stepFactory, newEntity, oldEntity));
		instanceVariables.add(getDmsIdStatement(stepFactory, newEntity, oldEntity));
		instanceVariables.add(getPurposeStatement(stepFactory, newEntity, oldEntity));
		// set parent of entity if available and present
		instanceVariables.add(getOwnedModelStep(stepFactory, newEntity, oldEntity));

		return instanceVariables.build();
	}

	private static PersistStep getDefinitionId(PersistStepFactory stepFactory, Instance newEntity, Instance oldEntity) {
		String oldIdentifier = null;
		if (oldEntity != null) {
			oldIdentifier = oldEntity.getIdentifier();
		}
		return stepFactory.create(EMF.DEFINITION_ID, newEntity.getIdentifier(), oldIdentifier);
	}

	private static PersistStep getDmsIdStatement(PersistStepFactory stepFactory, Instance newEntity,
			Instance oldEntity) {
		String newDmsId = DmsAware.getDmsId(newEntity, null);
		String oldDmsId = DmsAware.getDmsId(oldEntity, null);
		return stepFactory.create(EMF.DMS_ID, newDmsId, oldDmsId);
	}

	private static PersistStep getPurposeStatement(PersistStepFactory stepFactory, Instance newEntity,
			Instance oldEntity) {
		String newPurpose = Purposable.getPurpose(newEntity, null);
		String oldPurpose = Purposable.getPurpose(oldEntity, null);
		return stepFactory.create(EMF.PURPOSE, newPurpose, oldPurpose);
	}

	private PersistStep getOwnedModelStep(PersistStepFactory stepFactory, Instance entity, Instance old) {
		if (entity instanceof OwnedModel && !Options.DISABLE_AUTOMATIC_PARENT_CHILD_LINKS.isEnabled() && old == null) {
			// we cannot determine whether the parent has been changed or it's attached to new
			// parent but without removing the old one so the only thing we can do is the ensure the
			// link is not duplicated
			// NOTE: if add to removeModel is added all part of relations disappear so we only add
			// the partOf relations and the autolinkObserver adds the simple links
			return stepFactory.create(Proton.PART_OF, getOwningReferenceUri(entity), null);
		}
		return PersistStep.empty();
	}

	/**
	 * Gets the owning reference URI from the given instance or <code>null</code> if not present.
	 *
	 * @param instance
	 *            the instance
	 * @return the owning reference URI or <code>null</code>.
	 */
	private URI getOwningReferenceUri(Instance instance) {
		Serializable parentUri = null;
		if (instance instanceof OwnedModel) {
			parentUri = getParentId((OwnedModel) instance);
		}
		if (parentUri == null) {
			return null;
		}
		return namespaceRegistryService.buildUri(parentUri.toString());
	}

	/**
	 * Gets the parent instance id.
	 *
	 * @param ownedModel
	 *            the owned model
	 * @return the parent owning instance id
	 */
	private static Serializable getParentId(OwnedModel ownedModel) {
		if (ownedModel.getOwningInstance() != null) {
			return ownedModel.getOwningInstance().getId();
		} else if (ownedModel.getOwningReference() != null) {
			return ownedModel.getOwningReference().getIdentifier();
		}
		return null;
	}
}
