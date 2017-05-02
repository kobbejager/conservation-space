package com.sirma.itt.seip.rest.handlers.writers;

import static com.sirma.itt.seip.collections.CollectionUtils.isEmpty;
import static com.sirma.itt.seip.collections.CollectionUtils.isNotEmpty;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.HEADERS;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.HEADER_BREADCRUMB;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.HEADER_COMPACT;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.HEADER_DEFAULT;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.INSTANCE_TYPE;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.SEMANTIC_HIERARCHY;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.SEMANTIC_TYPE;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.THUMBNAIL_IMAGE;
import static com.sirma.itt.seip.instance.version.VersionProperties.HAS_VIEW_CONTENT;
import static com.sirma.itt.seip.instance.version.VersionProperties.IS_VERSION;
import static com.sirma.itt.seip.instance.version.VersionProperties.MANUALLY_SELECTED;
import static com.sirma.itt.seip.instance.version.VersionProperties.ORIGINAL_INSTANCE_ID;
import static com.sirma.itt.seip.instance.version.VersionProperties.QUERIES_RESULTS;
import static com.sirma.itt.seip.instance.version.VersionProperties.VERSION_CREATION_DATE;
import static com.sirma.itt.seip.rest.utils.JsonKeys.DEFINITION_ID;
import static com.sirma.itt.seip.rest.utils.JsonKeys.ID;
import static com.sirma.itt.seip.rest.utils.JsonKeys.INSTANCE_HEADERS;
import static com.sirma.itt.seip.rest.utils.JsonKeys.PARENT_ID;
import static com.sirma.itt.seip.rest.utils.JsonKeys.PROPERTIES;
import static com.sirma.itt.seip.util.EqualsHelper.getOrDefault;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.seip.ShortUri;
import com.sirma.itt.seip.Uri;
import com.sirma.itt.seip.collections.CollectionUtils;
import com.sirma.itt.seip.convert.TypeConverter;
import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.definition.SemanticDefinitionService;
import com.sirma.itt.seip.domain.codelist.CodelistService;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.PropertyDefinition;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.InstanceReference;
import com.sirma.itt.seip.domain.instance.OwnedModel;
import com.sirma.itt.seip.exception.EmfRuntimeException;
import com.sirma.itt.seip.instance.InstanceTypeResolver;
import com.sirma.itt.seip.instance.dao.InstanceLoadDecorator;
import com.sirma.itt.seip.rest.utils.JSON;
import com.sirma.itt.seip.rest.utils.JsonKeys;
import com.sirma.itt.seip.time.ISO8601DateFormat;

/**
 * {@link Instance} to {@link JsonObject} serializer.
 *
 * @author yasko
 * @author A. Kunchev
 */
@Singleton
public class InstanceToJsonSerializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/** Properties of relation instance that should be serialized. All others will be removed. */
	private static final PropertiesFilterBuilder RELATION_PROPERTIES_FILTER = new SelectedPropertiesFilter(
			Arrays.asList(HEADER_COMPACT, HEADER_DEFAULT, HEADER_BREADCRUMB));

	@Inject
	private DictionaryService definitions;

	@Inject
	private SemanticDefinitionService semanticDefinitionService;

	@Inject
	private TypeConverter typeConverter;

	@Inject
	private InstanceTypeResolver instanceTypeResolver;

	@Inject
	private InstanceLoadDecorator instanceLoadDecorator;

	@Inject
	private CodelistService codelistService;

	/**
	 * Creates predicate which could be used for properties filtering. The passed collection should contain only
	 * properties that should be added to the model. If the passed collection is empty or null, generated predicate will
	 * be: <b>{@code (k,v) -> false }</b>, which will remove all the properties from the instance.
	 *
	 * @param properties
	 *            properties to be filtered
	 * @return true if the passed properties contains the key of the entry, false otherwise
	 */
	public static PropertiesFilterBuilder onlyProperties(Collection<String> properties) {
		if (CollectionUtils.isEmpty(properties)) {
			return PropertiesFilterBuilder.MATCH_NONE;
		}

		Set<String> filter = new HashSet<>(properties);
		return new SelectedPropertiesFilter(filter);
	}

	/**
	 * Predicate that that matches all instance properties.
	 *
	 * @return the all properties filter
	 */
	public static PropertiesFilterBuilder allProperties() {
		return PropertiesFilterBuilder.MATCH_ALL;
	}

	/**
	 * Returns a predicate that will match the given properties or all properties if non are specified.
	 *
	 * @param projectionProperties
	 *            the projection properties, optional
	 * @return the predicate tha matches the all properties or the given list of properties
	 */
	public static PropertiesFilterBuilder allOrGivenProperties(Collection<String> projectionProperties) {
		// by default return all properties
		PropertiesFilterBuilder propertiesFilter = allProperties();
		if (isNotEmpty(projectionProperties)) {
			propertiesFilter = new SelectedPropertiesFilter(projectionProperties);
		}
		return propertiesFilter;
	}

	/**
	 * Converts the provided collection of {@link Instance}s to a {@link JsonObject} with its properties and relations.
	 * The method is optimized for batch loading of relations and should be used for serializing more than one instance.
	 *
	 * @param instances
	 *            the provided instances to serialize
	 * @param generator
	 *            the JSON generator where the instance is serialized
	 */
	public void serialize(Collection<Instance> instances, JsonGenerator generator) {
		serialize(instances, allProperties(), generator);
	}

	/**
	 * Converts the provided collection of {@link Instance}s to a {@link JsonObject} with its properties and relations.
	 * The method is optimized for batch loading of relations and should be used for serializing more than one instance.
	 *
	 * @param instances
	 *            the provided instances to serialize
	 * @param propertiesFilter
	 *            filter properties that should be added to the model. If you want to add to the model for example
	 *            'title', this predicate should return <b>true</b> for the 'title' test.
	 * @param generator
	 *            the JSON generator where the instance is serialized
	 * @see #onlyProperties(Collection)
	 */
	public void serialize(Collection<Instance> instances, PropertiesFilterBuilder propertiesFilter,
			JsonGenerator generator) {
		for (Instance instance : instances) {
			// iterate in the given order because the data returned from load is not ordered in any way
			serialize(instance, propertiesFilter, generator);
		}
	}

	/**
	 * Converts the provided {@link Instance} to a {@link JsonObject} with its properties and relations.
	 *
	 * @param instance
	 *            the provided instance
	 * @param generator
	 *            the JSON generator where the instance is serialized
	 */
	public void serialize(Instance instance, JsonGenerator generator) {
		serialize(instance, allProperties(), generator);
	}

	/**
	 * Converts the provided {@link Instance} to a {@link JsonObject} with its properties and relations.
	 *
	 * @param instance
	 *            the provided instance
	 * @param generator
	 *            the JSON generator where the instance is serialized
	 * @param objectName
	 *            - name for the key of the created json object. When null result is the same as
	 *            {@link #serialize(Instance, JsonGenerator)}
	 */
	public void serialize(Instance instance, JsonGenerator generator, String objectName) {
		if (objectName != null) {
			generator.writeStartObject(objectName);
		} else {
			generator.writeStartObject();
		}
		writeInstanceData(instance, allProperties(), generator);
		generator.writeEnd();
	}

	/**
	 * Writes the instance data to the given {@link JsonGenerator}. The data is written to a {@link JsonObject} with the
	 * given name. If this is not desired behavior use the {@link #serialize(Instance, JsonGenerator)} method that
	 * writes a simple {@link JsonObject}.
	 *
	 * @param fieldName
	 *            the name of the generated {@link JsonObject} where the instance data to be written
	 * @param instance
	 *            the provided instance
	 * @param generator
	 *            the JSON generator where the instance is serialized
	 */
	public void serialize(String fieldName, Instance instance, JsonGenerator generator) {
		generator.writeStartObject(fieldName);
		writeInstanceData(instance, allProperties(), generator);
		generator.writeEnd();
	}

	/**
	 * Converts the provided {@link Instance} to a {@link JsonObject} with its properties and relations. The properties
	 * could be filtered by passing the predicate, which returns true for the properties that should be added to the
	 * model.
	 *
	 * @param instance
	 *            the provided instance
	 * @param propertiesFilter
	 *            filter properties that should be added to the model. If you want to add to the model for example
	 *            'title', this predicate should return <b>true</b> for the 'title' test.
	 * @param generator
	 *            the JSON generator where the instance is serialized
	 */
	public void serialize(Instance instance, PropertiesFilterBuilder propertiesFilter, JsonGenerator generator) {
		generator.writeStartObject();
		writeInstanceData(instance, propertiesFilter, generator);
		generator.writeEnd();
	}

	private void writeInstanceData(Instance instance, PropertiesFilterBuilder requestedProperties,
			JsonGenerator generator) {
		writeTopLevelProperties(instance, generator);

		DefinitionModel model = definitions.getInstanceDefinition(instance);
		if (model == null) {
			throw new EmfRuntimeException("Missing instance definition for object with id: " + instance.getId());
		}

		Predicate<String> fieldsFilter = requestedProperties.buildFilter(model);
		writeHeaders(instance, fieldsFilter, generator);
		writeThumbnail(instance, generator);
		writeInstanceType(instance, generator);

		// wrap the requested properties in a filter that will remove any forbidden properties for serialization
		Predicate<String> propertiesFilter = new ForbiddenPropertiesFilter(requestedProperties).buildFilter(model);
		writeProperties(instance, model, propertiesFilter, generator);
	}

	private static void writeTopLevelProperties(Instance instance, JsonGenerator generator) {
		JSON.addIfNotNull(generator, ID, (String) instance.getId());
		JSON.addIfNotNull(generator, DEFINITION_ID, instance.getIdentifier());
		writeParentId(instance, generator);
		generator.write(JsonKeys.READ_ALLOWED, instance.isReadAllowed());
		generator.write(JsonKeys.WRITE_ALLOWED, instance.isWriteAllowed());
		if (instance.isDeleted()) {
			generator.write("deleted", instance.isDeleted());
		}
	}

	private static void writeParentId(Instance instance, JsonGenerator generator) {
		if (!(instance instanceof OwnedModel)) {
			return;
		}

		OwnedModel owned = (OwnedModel) instance;
		InstanceReference owner = owned.getOwningReference();
		if (owner != null) {
			JSON.addIfNotNull(generator, PARENT_ID, owner.getIdentifier());
			return;
		}
	}

	private void writeProperties(Instance instance, DefinitionModel model, Predicate<String> propertiesFilter,
			JsonGenerator generator) {
		generator.writeStartObject(PROPERTIES);
		writeVersionProperties(instance, generator);
		writeMandatoryProperties(instance, generator);
		filterAndWriteProperties(instance, model, propertiesFilter, generator);
		generator.writeEnd();
	}

	// we need this because there are specific version properties that aren't described in the model
	// and they are filtered. Thats why they should be serialized explicitly
	private static void writeVersionProperties(Instance instance, JsonGenerator generator) {
		if (!instance.getBoolean(IS_VERSION, false)) {
			return;
		}

		// most of this properties will be removed, when the functionality for idoc content processing is done
		generator
				.write(IS_VERSION, instance.getBoolean(IS_VERSION))
					.write(HAS_VIEW_CONTENT, instance.getBoolean(HAS_VIEW_CONTENT))
					.write(ORIGINAL_INSTANCE_ID, instance.getString(ORIGINAL_INSTANCE_ID));
		JSON.addIfNotNull(generator, MANUALLY_SELECTED, instance.getString(MANUALLY_SELECTED));
		String versionDateAsString = ISO8601DateFormat.format(instance.get(VERSION_CREATION_DATE, Date.class));
		JSON.addIfNotNull(generator, VERSION_CREATION_DATE, versionDateAsString);
		JSON.addIfNotNull(generator, QUERIES_RESULTS, instance.getString(QUERIES_RESULTS));
	}

	private void writeMandatoryProperties(Instance instance, JsonGenerator generator) {
		String semanticType = instance.getAsString(SEMANTIC_TYPE);
		if (semanticType != null) {
			writeSemanticHierarchy(generator, semanticType);
		} else if (instance.type() != null) {
			writeSemanticHierarchy(generator, instance.type().getId().toString());
		}
	}

	private void writeSemanticHierarchy(JsonGenerator generator, String semanticType) {
		List<String> semanticHierarchy = semanticDefinitionService.getHierarchy(semanticType);
		generator.writeStartArray(SEMANTIC_HIERARCHY);
		for (String semanticHierarchyType : semanticHierarchy) {
			generator.write(semanticHierarchyType);
		}
		generator.writeEnd();
	}

	private void filterAndWriteProperties(Instance instance, DefinitionModel definition,
			Predicate<String> allowedPropertiesFilter, JsonGenerator generator) {

		Predicate<PropertyDefinition> propertiesFilter = field -> instance.isPropertyPresent(field.getName())
				&& allowedPropertiesFilter.test(field.getName());

		// collect ids of instance that are as object properties that are requested and will be returned
		// so we will load only these instances and not all of them if not needed.
		Set<Serializable> referencedInstanceIds = definition
				.fieldsStream()
					.filter(propertiesFilter)
					.filter(PropertyDefinition.isObjectProperty())
					.map(PropertyDefinition::getName)
					.map(instance::get)
					.flatMap(this::toInstanceIds)
					.collect(Collectors.toSet());

		// map loaded instance by id for easy retrieval when writing object properties data
		Map<Serializable, Instance> referencedInstances = instanceTypeResolver
				.resolveInstances(referencedInstanceIds)
					.stream()
					.collect(Collectors.toMap(Instance::getId, Function.identity()));

		instanceLoadDecorator.decorateResult(referencedInstances.values());

		// write only allow properties
		definition.fieldsStream().filter(propertiesFilter).forEach(
				field -> writeProperty(field, instance.get(field.getName()), referencedInstances, generator));

		// write properties that are not in the model
		// probably this is not needed
		Map<String, PropertyDefinition> fieldsAsMap = definition.getFieldsAsMap();
		instance
				.getOrCreateProperties()
					.keySet()
					.stream()
					.filter(key -> !fieldsAsMap.containsKey(key))
					.filter(allowedPropertiesFilter)
					.forEach(name -> writeLiteralProperty(name, instance.get(name), generator));
	}

	@SuppressWarnings("unchecked")
	private Stream<Serializable> toInstanceIds(Serializable value) {
		if (value instanceof String || value instanceof Uri) {
			// convert single ids to short uri format
			String uri = Objects.toString(typeConverter.convert(ShortUri.class, value), null);
			return uri == null ? Stream.empty() : Stream.of(uri);
		} else if (value instanceof Collection) {
			// collections elements should be converted one by one
			return ((Collection<Serializable>) value).stream().flatMap(v -> toInstanceIds(v));
		}
		LOGGER.warn("Not supported type of object property value: {}", value);
		return Stream.empty();
	}

	private void writeProperty(PropertyDefinition field, Serializable value,
			Map<Serializable, Instance> referencedInstances, JsonGenerator generator) {
		if (PropertyDefinition.isObjectProperty().test(field)) {
			writeObjectProperty(field, value, referencedInstances, generator);
		} else {
			if (field.getCodelist() != null) {
				writeCodelistProperty(field.getName(), field.getCodelist(), value, generator);
			} else {
				writeLiteralProperty(field.getName(), value, generator);
			}
		}
	}

	private void writeCodelistProperty(String name, Integer codelist, Serializable value, JsonGenerator generator) {
		if (value instanceof String) {
			String description = codelistService.getDescription(codelist, (String) value);

			generator.writeStartObject(name);
			writeCodelistField(value.toString(), description, generator);
			generator.writeEnd();
		} else if (value instanceof Collection) {
			generator.writeStartArray(name);

			for (Object item : (Collection<?>) value) {
				String code = Objects.toString(item, "");
				String description = codelistService.getDescription(codelist, code);

				generator.writeStartObject();
				writeCodelistField(code, description, generator);
				generator.writeEnd();
			}

			generator.writeEnd();
		} else {
			LOGGER.warn(
					"Recieved non String and non collection literal for codelist property: name={}, CL={}, value={}",
					name, codelist, value);
		}
	}

	private static void writeCodelistField(String code, String label, JsonGenerator generator) {
		// uses this constants to be Select2 compatible
		generator.write("id", code);
		String description = getOrDefault(label, code);
		generator.write("text", description);
	}

	private void writeObjectProperty(PropertyDefinition field, Serializable value,
			Map<Serializable, Instance> referencedInstances, JsonGenerator generator) {
		List<Instance> relatedInstances = toInstanceIds(value)
				.map(referencedInstances::get)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		if (isEmpty(relatedInstances)) {
			return;
		}
		generator.writeStartArray(field.getName());
		for (Instance instance : relatedInstances) {
			serialize(instance, RELATION_PROPERTIES_FILTER, generator);
		}
		generator.writeEnd();
	}

	private void writeLiteralProperty(String name, Serializable value, JsonGenerator generator) {
		if (value instanceof Date) {
			generator.write(name, typeConverter.convert(String.class, value));
		} else if (value instanceof Long || value instanceof Integer) {
			generator.write(name, ((Number) value).longValue());
		} else if (value instanceof Float || value instanceof Double) {
			generator.write(name, Double.parseDouble(String.valueOf(value)));
		} else if (value instanceof Boolean) {
			generator.write(name, ((Boolean) value).booleanValue());
		} else if (value instanceof String) {
			generator.write(name, value.toString());
		} else {
			writeNonGenericLiteral(name, value, generator);
		}
	}

	private void writeNonGenericLiteral(String name, Serializable value, JsonGenerator generator) {
		if (value == null) {
			// no need to handle null values
			return;
		}
		JsonValue jsonValue = typeConverter.tryConvert(JsonValue.class, value);
		if (jsonValue != null) {
			generator.write(name, jsonValue);
			return;
		}
		String asString = typeConverter.tryConvert(String.class, value);
		if (!JSON.addIfNotNull(generator, name, asString)) {
			LOGGER.warn("Could not convert value [{}] to supported JSON format!", value);
		}
	}

	private static void writeHeaders(Instance instance, Predicate<String> allowedPropertiesFilter,
			JsonGenerator generator) {
		generator.writeStartObject(INSTANCE_HEADERS);
		HEADERS.stream().filter(allowedPropertiesFilter).forEach(
				header -> JSON.addIfNotNull(generator, header, instance.getString(header)));
		generator.writeEnd();
	}

	/**
	 * The thumbnail is extracted and set into the model, because it is filtered, when the instance properties are
	 * serialized.
	 *
	 * @param instance
	 *            the instance from which the thumbnail will be extracted
	 * @param generator
	 *            {@link JsonGenerator} which builds the model
	 */
	private static void writeThumbnail(Instance instance, JsonGenerator generator) {
		String thumbnailImage = instance.getString(THUMBNAIL_IMAGE);
		JSON.addIfNotNull(generator, THUMBNAIL_IMAGE, thumbnailImage);
	}

	private static void writeInstanceType(Instance instance, JsonGenerator generator) {
		// FIXME: we should no longer have to depend on this
		JSON.addIfNotNull(generator, INSTANCE_TYPE, instance.type().getCategory());
	}
}