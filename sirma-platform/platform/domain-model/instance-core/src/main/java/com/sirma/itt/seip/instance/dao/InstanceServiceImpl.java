package com.sirma.itt.seip.instance.dao;

import static com.sirma.itt.seip.collections.CollectionUtils.emptySet;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.NOT_CLONABLE_PROPERTIES;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.SEMANTIC_TYPE;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.seip.collections.CollectionUtils;
import com.sirma.itt.seip.configuration.Options;
import com.sirma.itt.seip.db.RelationalDb;
import com.sirma.itt.seip.definition.AllowedChildrenProvider;
import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.definition.SemanticDefinitionService;
import com.sirma.itt.seip.definition.TypeMappingProvider;
import com.sirma.itt.seip.domain.ObjectTypes;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.PropertyDefinition;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.InstanceReference;
import com.sirma.itt.seip.domain.instance.OwnedModel;
import com.sirma.itt.seip.domain.security.ActionTypeConstants;
import com.sirma.itt.seip.domain.util.PropertiesUtil;
import com.sirma.itt.seip.event.EventService;
import com.sirma.itt.seip.event.TwoPhaseEvent;
import com.sirma.itt.seip.instance.actions.InstanceOperations;
import com.sirma.itt.seip.instance.archive.ArchiveService;
import com.sirma.itt.seip.instance.event.AfterInstancePersistEvent;
import com.sirma.itt.seip.instance.event.BeforeInstancePersistEvent;
import com.sirma.itt.seip.instance.event.InstanceEventProvider;
import com.sirma.itt.seip.instance.relation.LinkConstants;
import com.sirma.itt.seip.instance.relation.LinkReference;
import com.sirma.itt.seip.instance.relation.LinkService;
import com.sirma.itt.seip.instance.revision.RevisionService;
import com.sirma.itt.seip.instance.state.Operation;
import com.sirma.itt.seip.instance.state.OperationExecutedEvent;
import com.sirma.itt.seip.instance.validation.ValidationContext;
import com.sirma.itt.seip.instance.validation.Validator;
import com.sirma.itt.seip.time.TimeTracker;

/**
 * Default implementation of {@link InstanceService}
 *
 * @author BBonev
 */
@ApplicationScoped
public class InstanceServiceImpl implements InstanceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final Operation CREATE = new Operation(ActionTypeConstants.CREATE);

	private static final Predicate<PropertyDefinition> IS_PROPERTY_RDF_TYPE = prop -> SEMANTIC_TYPE
			.equals(prop.getName());

	@Inject
	private EventService eventService;

	@Inject
	private DictionaryService dictionaryService;

	@Inject
	private TypeMappingProvider typeProvider;

	@Inject
	private ServiceRegistry serviceRegistry;

	@Inject
	private ArchiveService archiveService;

	@Inject
	private InstanceOperations operationInvoker;

	@Inject
	private InstanceLoadDecorator instanceLoadDecorator;

	@Inject
	private Validator validatorService;

	@Inject
	private SemanticDefinitionService semanticDefinitionService;

	@Inject
	@InstanceType(type = ObjectTypes.OBJECT)
	private InstanceDao instanceDao;

	@Inject
	private RevisionService revisionService;

	@Inject
	@RelationalDb
	private LinkService linkService;

	/**
	 * Save instance.
	 *
	 * @param instance
	 *            the instance
	 * @param operation
	 *            the operation
	 * @return the project instance
	 */
	@SuppressWarnings("boxing")
	private Instance saveInstance(Instance instance, Operation operation) {
		// 1. call the adapter 2. Convert to caseEntity 3.persist data, 4.
		// persist properties
		if (operation != null) {
			Options.CURRENT_OPERATION.set(operation);
		}
		TimeTracker tracker = TimeTracker.createAndStart();
		validatorService.validate(new ValidationContext(instance, operation));

		try {
			InstanceEventProvider<Instance> eventProvider = serviceRegistry.getEventProvider(instance);
			// update project instance state if needed
			eventService.fire(new OperationExecutedEvent(operation, instance));

			boolean onCreate = false;
			BeforeInstancePersistEvent<Instance, ? extends AfterInstancePersistEvent<Instance, TwoPhaseEvent>> persistEvent = null;

			// set the properties that relates to modifications but does not
			// save it
			instanceDao.instanceUpdated(instance, false);

			if (isSaveForCreate(instance, operation)) {
				onCreate = true;
				if (eventProvider != null) {
					eventService.fire(eventProvider.createChangeEvent(instance));
					persistEvent = eventProvider.createBeforeInstancePersistEvent(instance);
				}

				eventService.fire(persistEvent);
			} else {
				if (eventProvider != null) {
					eventService.fire(eventProvider.createChangeEvent(instance));
				}
			}

			// persist entity and properties
			Instance old = instanceDao.persistChanges(instance);

			if (onCreate) {
				eventService.fireNextPhase(persistEvent);
			}
			if (!Options.DO_NOT_FIRE_PERSIST_EVENT.isEnabled() && eventProvider != null) {
				eventService
						.fire(eventProvider.createPersistedEvent(instance, old, Operation.getOperationId(operation)));
			}
		} finally {
			LOGGER.debug("Total instance {} save took {} ms", instance.getId(), tracker.stop());
			if (operation != null) {
				Options.CURRENT_OPERATION.clear();
			}
		}
		return instance;
	}

	/**
	 * Checks if the instance is saved for create (first time) or normal save.
	 *
	 * @param instance
	 *            the instance
	 * @param operation
	 *            the operation
	 * @return true, if is save for create
	 */
	@SuppressWarnings("static-method")
	protected boolean isSaveForCreate(Instance instance, Operation operation) {
		// this is not ideal check but can't think of something better for now
		String operationId = Operation.getOperationId(operation);
		return instance.getId() == null
				|| operationId != null && (operationId.toLowerCase().contains(ActionTypeConstants.CREATE)
						|| operationId.toLowerCase().contains(ActionTypeConstants.IMPORT)
						|| operationId.toLowerCase().contains(ActionTypeConstants.CLONE));
	}

	/**
	 * Batch load instances.
	 *
	 * @param <S>
	 *            the generic type
	 * @param dmsIds
	 *            the dms ids
	 * @param loadAllProperties
	 *            the load all properties
	 * @return the list
	 */
	private <S extends Serializable> List<Instance> batchLoadInstances(List<S> dmsIds, boolean loadAllProperties) {
		if (dmsIds == null || dmsIds.isEmpty()) {
			return Collections.emptyList();
		}
		return instanceDao.loadInstances(dmsIds, loadAllProperties);
	}

	/**
	 * Batch load instances by id.
	 *
	 * @param <S>
	 *            the generic type
	 * @param ids
	 *            the ids
	 * @param loadAllProperties
	 *            the load all properties
	 * @return the list
	 */
	private <S extends Serializable> List<Instance> batchLoadInstancesById(List<S> ids, boolean loadAllProperties) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}

		return instanceDao.loadInstancesByDbKey(ids, loadAllProperties);
	}

	@Override
	public Instance createInstance(DefinitionModel definition, Instance parent) {
		return createInstance(definition, parent, CREATE);
	}

	@Override
	public Instance createInstance(DefinitionModel definition, Instance parent, Operation operation) {
		Objects.requireNonNull(definition, "Definition is required!");
		Instance instance = instanceDao.createInstance(definition, true);

		instanceDao.instanceUpdated(instance, false);
		// set the initial state
		eventService.fire(new OperationExecutedEvent(operation, instance));
		InstanceEventProvider<Instance> eventProvider = serviceRegistry.getEventProvider(instance);
		if (eventProvider != null) {
			eventService.fire(eventProvider.createCreateEvent(instance));
		}

		// fixes not set revision
		instanceDao.synchRevisions(instance, instance.getRevision());

		if (instance instanceof OwnedModel) {
			OwnedModel ownedModel = (OwnedModel) instance;
			if (ownedModel.getOwningInstance() == null) {
				ownedModel.setOwningInstance(parent);
			}
		}
		return instance;
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public Instance save(Instance instance, Operation operation) {
		Instance savedInstance = saveInstance(instance, operation);
		instanceLoadDecorator.decorateInstance(savedInstance);
		return savedInstance;
	}

	@Override
	public List<Instance> loadInstances(com.sirma.itt.seip.domain.instance.Instance owner) {
		return Collections.emptyList();
	}

	@Override
	public Instance loadByDbId(Serializable id) {
		Instance instance = instanceDao.loadInstance(id, null, true);
		instanceLoadDecorator.decorateInstance(instance);
		return instance;
	}

	@Override
	public Instance load(Serializable instanceId) {
		Instance instance = instanceDao.loadInstance(null, instanceId, true);
		instanceLoadDecorator.decorateInstance(instance);
		return instance;
	}

	@Override
	public <S extends Serializable> List<Instance> load(List<S> ids) {
		return batchLoadInstances(ids, true);
	}

	@Override
	public <S extends Serializable> List<Instance> loadByDbId(List<S> ids) {
		return batchLoadInstancesById(ids, true);
	}

	@Override
	public <S extends Serializable> List<Instance> load(List<S> ids, boolean allProperties) {
		return batchLoadInstances(ids, allProperties);
	}

	@Override
	public <S extends Serializable> List<Instance> loadByDbId(List<S> ids, boolean allProperties) {
		return batchLoadInstancesById(ids, allProperties);
	}

	@Override
	public Map<String, List<DefinitionModel>> getAllowedChildren(Instance owner) {
		// TODO: implement more specific provider
		AllowedChildrenProvider<Instance> calculator = new BaseAllowedChildrenProvider<>(dictionaryService,
				typeProvider);
		return AllowedChildrenHelper.getAllowedChildren(owner, calculator, dictionaryService);
	}

	@Override
	public List<DefinitionModel> getAllowedChildren(Instance owner, String type) {
		// TODO: implement more specific provider
		AllowedChildrenProvider<Instance> calculator = new BaseAllowedChildrenProvider<>(dictionaryService,
				typeProvider);
		return AllowedChildrenHelper.getAllowedChildren(owner, calculator, dictionaryService, type);
	}

	@Override
	public boolean isChildAllowed(Instance owner, String type) {
		AllowedChildrenProvider<Instance> calculator = new BaseAllowedChildrenProvider<>(dictionaryService,
				typeProvider);
		return AllowedChildrenHelper.isChildAllowed(owner, calculator, dictionaryService, type);
	}

	@Override
	public boolean isChildAllowed(Instance owner, String type, String definitionId) {
		AllowedChildrenProvider<Instance> calculator = new BaseAllowedChildrenProvider<>(dictionaryService,
				typeProvider);
		return AllowedChildrenHelper.isDefinitionChildAllowed(owner, calculator, dictionaryService, type, definitionId);
	}

	@Override
	public void refresh(Instance instance) {
		instanceDao.loadProperties(instance);
		instanceLoadDecorator.decorateInstance(instance);
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public Instance cancel(Instance instance) {
		return save(instance, new Operation(ActionTypeConstants.STOP));
	}

	@Override
	public Instance clone(Instance instanceToClone, Operation operation) {
		Instance instance = cloneInternal(instanceToClone, operation, NOT_CLONABLE_PROPERTIES, true);
		instance.getProperties().keySet().removeAll(semanticDefinitionService.getRelationsMap().keySet());
		eventService.fire(new OperationExecutedEvent(operation, instanceToClone));
		return instance;
	}

	@Override
	public Instance deepClone(Instance instanceToClone, Operation operation) {
		Instance clone = cloneInternal(instanceToClone, operation, emptySet(), false);
		eventService.fire(new OperationExecutedEvent(operation, instanceToClone));
		return clone;
	}

	private Instance cloneInternal(Instance instanceToClone, Operation operation, Set<String> propertiesToSkip,
			boolean removeObjectProperties) {
		Objects.requireNonNull(instanceToClone, "There should be instance to clone.");
		Objects.requireNonNull(operation, "The operation is required.");

		DefinitionModel instanceDefinition = dictionaryService.getInstanceDefinition(instanceToClone);
		Instance instance = createInstance(instanceDefinition, null, operation);
		instance.addAllProperties(PropertiesUtil.cloneProperties(instanceToClone.getProperties(), propertiesToSkip));
		if (removeObjectProperties) {
			instanceDefinition
					.fieldsStream()
						.filter(PropertyDefinition.isObjectProperty().and(IS_PROPERTY_RDF_TYPE.negate()))
						.map(PropertyDefinition::getName)
						.forEach(instance::remove);
		}

		return instance;
	}

	@Override
	public void delete(Instance instance, Operation operation, boolean permanent) {
		archiveService.scheduleDelete(instance, operation, permanent);
	}

	@Override
	public void attach(Instance targetInstance, Operation operation, Instance... children) {
		operationInvoker.invokeAttach(targetInstance, operation, children);
	}

	@Override
	public void detach(Instance sourceInstance, Operation operation, Instance... instances) {
		operationInvoker.invokeDetach(sourceInstance, operation, instances);
	}

	@Override
	public void touchInstance(Object object) {
		instanceDao.touchInstance(object);
	}

	@Override
	public InstanceReference getPrimaryParent(InstanceReference reference) {
		if (!InstanceReference.isValid(reference)) {
			return null;
		}
		List<LinkReference> parents = linkService.getLinks(reference, LinkConstants.HAS_PRIMARY_PARENT);
		if (CollectionUtils.isEmpty(parents)) {
			return null;
		}
		return parents.get(0).getTo();
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public Instance publish(Instance instance, Operation operation) {
		if (isOperationSet(operation)) {
			Options.CURRENT_OPERATION.set(operation);
		}

		try {
			return revisionService.publish(instance, operation);
		} finally {
			Options.CURRENT_OPERATION.clear();
		}
	}

	/**
	 * Checks if is operation set.
	 *
	 * @param operation
	 *            the operation
	 * @return true, if is operation set
	 */
	private static boolean isOperationSet(Operation operation) {
		return Operation.isSet(operation);
	}

	@Override
	public Optional<Instance> loadDeleted(Serializable id) {
		Options.ALLOW_LOADING_OF_DELETED_INSTANCES.enable();
		try {
			Instance instance = instanceDao.loadInstance(id, null, true);
			instanceLoadDecorator.decorateInstance(instance);
			return Optional.ofNullable(instance);
		} finally {
			Options.ALLOW_LOADING_OF_DELETED_INSTANCES.disable();
		}
	}
}
