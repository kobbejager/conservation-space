package com.sirma.itt.seip.instance.dao;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.seip.Entity;
import com.sirma.itt.seip.Pair;
import com.sirma.itt.seip.cache.lookup.EntityLookupCache;
import com.sirma.itt.seip.collections.CollectionUtils;
import com.sirma.itt.seip.configuration.Options;
import com.sirma.itt.seip.convert.TypeConverter;
import com.sirma.itt.seip.db.DbDao;
import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.domain.PathElement;
import com.sirma.itt.seip.domain.TenantAware;
import com.sirma.itt.seip.domain.definition.DataTypeDefinition;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.PropertyDefinition;
import com.sirma.itt.seip.domain.definition.RegionDefinition;
import com.sirma.itt.seip.domain.definition.RegionDefinitionModel;
import com.sirma.itt.seip.domain.exceptions.StaleDataModificationException;
import com.sirma.itt.seip.domain.instance.DefaultProperties;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.OwnedModel;
import com.sirma.itt.seip.domain.instance.PropertyModel;
import com.sirma.itt.seip.domain.rest.EmfApplicationException;
import com.sirma.itt.seip.event.EventService;
import com.sirma.itt.seip.instance.InstanceTypes;
import com.sirma.itt.seip.instance.event.InstanceChangeEvent;
import com.sirma.itt.seip.instance.event.InstanceCreateEvent;
import com.sirma.itt.seip.instance.event.InstanceEventProvider;
import com.sirma.itt.seip.model.InstanceEntity;
import com.sirma.itt.seip.resources.EmfUser;
import com.sirma.itt.seip.resources.ResourceService;
import com.sirma.itt.seip.time.TimeTracker;
import com.sirma.itt.seip.util.ReflectionUtils;

/**
 * Common implementation logic for {@link InstanceDao} realizations.
 *
 * @param <T>
 *            the instance type
 * @param <C>
 *            the entity type
 * @param <P>
 *            the primary key type
 * @param <K>
 *            the secondary key type
 * @param <D>
 *            the target definition type
 * @author BBonev
 */
@SuppressWarnings("rawtypes")
public abstract class BaseInstanceDaoImpl<C extends Entity, P extends Serializable, K extends Serializable, D extends DefinitionModel>
		extends BaseInstanceDao<P, K> implements InstanceDao {

	@Inject
	private DbDao dbDao;

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Inject
	protected DictionaryService dictionaryService;

	@Inject
	protected TypeConverter typeConverter;

	@Inject
	protected ServiceRegistry serviceRegistry;

	@Inject
	protected EventService eventService;

	@Inject
	protected ResourceService resourceService;

	@Inject
	private InstanceLoader instanceLoader;

	@Inject
	private com.sirma.itt.seip.security.context.SecurityContextManager securityContextManager;

	@Inject
	private InstanceTypes instanceTypes;

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Instance> T createInstance(DefinitionModel definitionModel, boolean populateProperties) {
		T instance = createNewInstance();
		if (definitionModel != null) {
			String identifier = definitionModel.getIdentifier();
			instance.setIdentifier(identifier);

			populateInstanceForModelInternal(instance, (D) definitionModel, populateProperties);
			instance.setType(instanceTypes.from(definitionModel).orElse(null));
		}
		return instance;
	}

	@Override
	public void synchRevisions(Instance instance, Long revision) {
		instance.setRevision(revision);
	}

	/**
	 * Notify for new instance.
	 *
	 * @param instance
	 *            the instance
	 */
	protected void notifyForNewInstance(Instance instance) {
		// better not to fire an event for not initialized instance
		if (instance == null || instance.getProperties() == null) {
			return;
		}
		InstanceEventProvider<Instance> provider = serviceRegistry.getEventProvider(instance);
		if (provider != null) {
			InstanceCreateEvent<Instance> event = provider.createCreateEvent(instance);
			if (event != null) {
				eventService.fire(event);
			}
		}
	}

	@Override
	public <T extends Instance> T createInstance(String definitionId, boolean populateProperties) {
		T instance = createNewInstance();

		if (definitionId != null) {
			instance.setIdentifier(definitionId);
			D model = getInstanceDefinition(definitionId);
			populateInstanceForModelInternal(instance, model, populateProperties);
		}
		return instance;
	}

	/**
	 * Creates the new instance. If the primary type is of type String then new ID is generated before return.
	 *
	 * @return a newly created instance.
	 */
	protected <T extends Instance> T createNewInstance() {
		T instance = ReflectionUtils.newInstance(getInstanceClass());
		// generate instance DB id
		if (getPrimaryIdType().equals(String.class)) {
			idManager.generateStringId(instance, true);
		}
		return instance;
	}

	/**
	 * Update newly created instance with model. When all base logic is called then the methods
	 * {@link #synchRevisions(Instance, Long)} and {@link #populateInstanceForModel(Instance, DefinitionModel)} is
	 * called for concrete additional implementations to fill any other data.
	 *
	 * @param instance
	 *            the instance
	 * @param model
	 *            the model
	 * @param populateProperties
	 *            the populate properties
	 */
	protected void populateInstanceForModelInternal(Instance instance, D model, boolean populateProperties) {
		if (model != null && populateProperties) {
			if (instance instanceof TenantAware) {
				((TenantAware) instance).setContainer(securityContext.getCurrentTenantId());
			}
			synchRevisions(instance, model.getRevision());
			populateInstanceForModel(instance, model);
		}
	}

	/**
	 * Gets the instance definition.
	 *
	 * @param definitionId
	 *            the definition id
	 * @return the instance definition
	 */
	@SuppressWarnings("unchecked")
	protected D getInstanceDefinition(String definitionId) {
		return (D) dictionaryService.find(definitionId);
	}

	@Override
	public void populateProperties(PropertyModel model, DefinitionModel definitionModel) {
		if (definitionModel != null) {
			super.populateProperties(model, definitionModel.getFields());
		}
	}

	@Override
	public void populateProperties(PropertyModel model, RegionDefinitionModel regionDefinitionModel) {
		if (regionDefinitionModel != null) {
			super.populateProperties(model, regionDefinitionModel.getFields());
			for (RegionDefinition regionDefinition : regionDefinitionModel.getRegions()) {
				super.populateProperties(model, regionDefinition.getFields());
			}
		}
	}

	@Override
	public void instanceUpdated(Instance instance, boolean autosave) {
		checkForStaleData(instance);

		onInstanceUpdated(instance);

		updateCreatorAndModifierInfo(instance);

		onAutoSave(instance, autosave);
	}

	/**
	 * Update creator and modifier info. There is an option to disable setting of modifier info
	 *
	 * @param instance
	 *            the instance
	 */
	protected void updateCreatorAndModifierInfo(Instance instance) {
		if (Options.AUDIT_MODIFICATION_DISABLED.isEnabled() || Options.OVERRIDE_MODIFIER_INFO.isEnabled()) {
			// disable the default implementation and allow external setting of modifier info
			return;
		}
		// use one date for modified and created
		Date currentDate = new Date();
		updateCreatorInfo(instance, currentDate);

		updateModifierInfo(instance, currentDate);
	}

	/**
	 * Update modifier info.
	 *
	 * @param instance
	 *            the instance
	 * @param currentDate
	 *            the current date
	 */
	protected void updateModifierInfo(Instance instance, Date currentDate) {
		// set modifier info
		setCurrentUserTo(instance, DefaultProperties.MODIFIED_BY);
		instance.add(DefaultProperties.MODIFIED_ON, currentDate);
	}

	/**
	 * Update creator info.
	 *
	 * @param instance
	 *            the instance
	 * @param currentDate
	 *            the current date
	 */
	protected void updateCreatorInfo(Instance instance, Date currentDate) {
		// if already created does not set them again
		instance.addIfNullMapping(DefaultProperties.CREATED_ON, currentDate);
		instance.addIfNullMapping(DefaultProperties.CREATED_BY, securityContext.getAuthenticated().getSystemId());
	}

	/**
	 * On auto save.
	 *
	 * @param instance
	 *            the instance
	 * @param autosave
	 *            the auto save
	 */
	protected void onAutoSave(Instance instance, boolean autosave) {
		// base implementation
		if (autosave) {
			persistChanges(instance);
		}
	}

	/**
	 * Method call on every all of {@link #instanceUpdated(Instance, boolean)} right after the check for stale data.
	 *
	 * @param instance
	 *            the instance
	 */
	protected void onInstanceUpdated(Instance instance) {
		// override to add functionality. No defaults.
	}

	@Override
	public boolean isModified(Instance instance) {
		return isModifiedInternal(instance, false);
	}

	/**
	 * Checks if the given instance is modified the method could optionally throw an exception if needed on detected
	 * modification instead of returning <code>true</code>.
	 *
	 * @param instance
	 *            the instance
	 * @param throwException
	 *            the throw exception
	 * @return true, if is modified internal
	 * @see {@link #checkForStaleData(Instance)}
	 * @see #getModifiedBy(Map)
	 * @see #getModifiedDate(Map)
	 * @see #checkDatesForChanges(Date, Date)
	 * @see #isLastModificationImportant(Instance, Map, Serializable)
	 */
	protected boolean isModifiedInternal(Instance instance, boolean throwException) {
		// get the current (old) timestamp
		Date currentModifiedOn = getModifiedDate(instance.getProperties());
		// get from the DB the latest known properties
		Date oldModifiedOn = null;
		Serializable oldModifiedBy = null;

		Map<String, Serializable> oldProperties = getLastKnownProperties(instance);
		// if no previous data then we are done
		if (oldProperties == null) {
			return false;
		}

		oldModifiedOn = getModifiedDate(oldProperties);
		oldModifiedBy = getModifiedBy(oldProperties);

		// if we already have a modified date and the incoming instance does not have or is the
		// cache entry is newer that the incoming modified date then we have race condition.

		if (checkDatesForChanges(currentModifiedOn, oldModifiedOn)
				&& !isLastModificationImportant(instance, oldProperties, oldModifiedBy)) {
			StringBuilder builder = new StringBuilder(256)
					.append("Trying to save ")
						.append(instance.getClass().getSimpleName())
						.append(" with id ")
						.append(instance.getId())
						.append(" that has stale data. Last modified by ")
						.append(oldModifiedBy)
						.append(". Current time ")
						.append(typeConverter.convert(String.class, currentModifiedOn))
						.append(" and the last known is ")
						.append(typeConverter.convert(String.class, oldModifiedOn));
			// throw exception if desired or print message in log.
			if (throwException) {
				throw new StaleDataModificationException(builder.toString(), oldModifiedBy, oldModifiedOn);
			}
			LOGGER.info(builder.toString());
			return true;
		}
		return false;
	}

	/**
	 * Check dates for changes.
	 *
	 * @param currentModifiedOn
	 *            the current modified on
	 * @param oldModifiedOn
	 *            the old modified on
	 * @return true, if there are changes in the modified dates
	 */
	protected boolean checkDatesForChanges(Date currentModifiedOn, Date oldModifiedOn) {
		return isAlreadyCreated(currentModifiedOn, oldModifiedOn)
				|| isOldModifiedDateIsAfterCurrent(currentModifiedOn, oldModifiedOn);
	}

	/**
	 * Checks if is old modified date is after current modified date
	 *
	 * @param currentModifiedOn
	 *            the current modified on
	 * @param oldModifiedOn
	 *            the old modified on
	 * @return true, if is old modified date is after current
	 */
	@SuppressWarnings("static-method")
	protected boolean isOldModifiedDateIsAfterCurrent(Date currentModifiedOn, Date oldModifiedOn) {
		return currentModifiedOn != null && oldModifiedOn != null
				&& oldModifiedOn.getTime() > currentModifiedOn.getTime();
	}

	/**
	 * Checks if is already created.
	 *
	 * @param currentModifiedOn
	 *            the current modified on
	 * @param oldModifiedOn
	 *            the old modified on
	 * @return true, if is already created
	 */
	@SuppressWarnings("static-method")
	protected boolean isAlreadyCreated(Date currentModifiedOn, Date oldModifiedOn) {
		return oldModifiedOn != null && currentModifiedOn == null;
	}

	/**
	 * This method is called when change, made by the given user, is detected in the passed instance. The default
	 * implementation checks if the user is system and marks it as non important and could be overridden.
	 *
	 * @param instance
	 *            the instance modified instance
	 * @param oldProperties
	 *            the old properties if any. Contains the last known properties of the instance before this change.
	 * @param lastModifier
	 *            the last modifier
	 * @return <code>true</code> if the last change should not be allowed to be overridden. If <code>false</code> the
	 *         last change will be lost and overridden.
	 */
	protected boolean isLastModificationImportant(Instance instance, Map<String, Serializable> oldProperties,
			Serializable lastModifier) {
		if (lastModifier == null) {
			return false;
		}
		com.sirma.itt.seip.security.User systemUser = securityContextManager.getSystemUser();
		return resourceService.areEqual(lastModifier, new EmfUser(systemUser));
	}

	/**
	 * <b>NOTE:</b> this method will work properly only if called before updating the last modified date of an instance.
	 * <p>
	 * Checks the given instance if it has stale data. The implementation uses the information provided from the method
	 * {@link #getLastKnownProperties(Instance)} to determine the latest modified date and modifier of the instance and
	 * the information in the current instance to compare them.<br>
	 * There are two supported fail scenarios:
	 * <ol>
	 * <li>The current instance does not have a modified date but the latest from the cache has. This is considered that
	 * the current instance is going to be created but already is.
	 * <li>The second scenario is if the modified date of the current instance is before (less than) the one from the
	 * cache.
	 * </ol>
	 * If any of the cases is met {@link StaleDataModificationException} is thrown
	 *
	 * @param instance
	 *            the instance to check
	 * @see #getModifiedDate(Map)
	 * @see #getLastKnownProperties(Instance)
	 * @see isModifiedInternal
	 */
	protected void checkForStaleData(Instance instance) {
		boolean set = Options.DISABLE_STALE_DATA_CHECKS.isEnabled();
		isModifiedInternal(instance, !set);
	}

	/**
	 * Gets the modified by.
	 *
	 * @param properties
	 *            the properties
	 * @return the modified by
	 */
	@SuppressWarnings("static-method")
	protected Serializable getModifiedBy(Map<String, Serializable> properties) {
		Serializable currentModifiedOn = null;
		if (properties != null) {
			currentModifiedOn = properties.get(DefaultProperties.MODIFIED_BY);
		}
		return currentModifiedOn;
	}

	/**
	 * Gets the modified date from the given properties. Default property returned is
	 * {@link DefaultProperties#MODIFIED_ON}.
	 *
	 * @param properties
	 *            the instance
	 * @return the modified date or <code>null</code>
	 */
	@SuppressWarnings("static-method")
	protected Date getModifiedDate(Map<String, Serializable> properties) {
		Date currentModifiedOn = null;
		if (properties != null) {
			currentModifiedOn = (Date) properties.get(DefaultProperties.MODIFIED_ON);
		}
		return currentModifiedOn;
	}

	/**
	 * Gets the latest known properties for the given instance. The method should return the last reliable known
	 * properties before the change. The default implementation tries to fetch the latest properties using
	 * {@link com.sirma.itt.seip.instance.properties.PropertiesService}. If the concrete instance does not support
	 * {@link com.sirma.itt.seip.instance.properties.PropertiesService} then new instance is loaded using
	 * {@link #loadInstance(Serializable, Serializable, boolean)} and properties are get from there.
	 *
	 * @param instance
	 *            the instance
	 * @return the latest known modified date and user
	 */
	protected Map<String, Serializable> getLastKnownProperties(Instance instance) {
		// the id check is for instances that does not have a DB generated before save
		// for them we can't actually load them or identify that are already persist
		if (propertiesService.isModelSupported(instance) && instance.getId() != null) {
			return propertiesService.getEntityProperties(instance, instance);
		} else if (idManager.isPersisted(instance)) {
			// no need to query anything if not persisted
			Instance latest = loadInstance(instance.getId(),
					getInstanceLoader().getPersistCallback().getSecondaryIdLoadHandler().getId(instance), true);
			if (latest != null) {
				return latest.getProperties();
			}
		}
		return null;
	}

	@Override
	public <T extends Instance> T persistChanges(T instance) {
		if (String.class.equals(getPrimaryIdType())) {
			idManager.generateStringId(instance, true);
		}

		boolean debug = LOGGER.isDebugEnabled();
		boolean trace = LOGGER.isTraceEnabled();

		TimeTracker tracker = new TimeTracker();
		if (debug || trace) {
			tracker.begin().begin();
		}
		// convert and persists the base case data
		T old = convertAndSave(instance);
		if (trace) {
			LOGGER.trace("{} save took {} sec", getInstanceClass().getSimpleName(), tracker.stopInSeconds());
			tracker.begin();
		}

		// to persist properties the instance need to be persisted
		savePropertiesOnPersistChanges(instance);
		if (debug) {
			if (trace) {
				LOGGER.trace("{} properties save {} sec", getInstanceClass().getSimpleName(), tracker.stopInSeconds());
			}
			LOGGER.debug("{} total save time is {} sec", getInstanceClass().getSimpleName(), tracker.stopInSeconds());
		}
		return old;
	}

	/**
	 * Save properties on persist changes with default save mode of Replace.
	 *
	 * @param instance
	 *            the instance
	 */
	protected void savePropertiesOnPersistChanges(Instance instance) {
		boolean addOnly = Options.ADD_ONLY_PROPERTIES.isEnabled();
		saveProperties(instance, addOnly);
	}

	@Override
	public void setCurrentUserTo(Instance model, String key) { // NOSONAR
		super.setCurrentUserTo(model, key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Instance> T loadInstance(Serializable id, Serializable dmsId, boolean loadProperties) {
		return loadInstanceInternal((P) id, (K) dmsId);
	}

	@Override
	public <S extends Serializable, T extends Instance> List<T> loadInstances(List<S> dmsIds) {
		return loadInstances(dmsIds, true);
	}

	@Override
	public <S extends Serializable, T extends Instance> List<T> loadInstances(List<S> dmsIds,
			boolean loadAllProperties) {
		return onBatchInstanceLoad(getInstanceLoader().loadBySecondaryId(dmsIds));
	}

	/**
	 * Method called before instances are returted from batch load method.
	 *
	 * @param instances
	 *            the instances
	 * @return the list
	 */
	protected <T extends Instance> List<T> onBatchInstanceLoad(Collection<? extends Instance> instances) {
		if (instances instanceof List) {
			return List.class.cast(instances);
		}
		return new ArrayList<>(Collection.class.cast(instances));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends Serializable, T extends Instance> List<T> loadInstancesByDbKey(List<S> ids,
			boolean loadAllProperties) {
		return (List<T>) getInstanceLoader().load(ids);
	}

	@Override
	public void loadProperties(Instance instance) {
		propertiesService.loadProperties(instance);
	}

	/**
	 * Load headers.
	 *
	 * @param <E>
	 *            the element type
	 * @param instance
	 *            the instance
	 * @param toLoadProps
	 *            the to load props
	 * @param ifMissing
	 *            to generate the headers only if missing
	 */
	protected <E extends Instance> void loadHeadersInternal(E instance, List<E> toLoadProps, boolean ifMissing) {
		TimeTracker tracker = new TimeTracker();
		boolean trace = LOGGER.isTraceEnabled();
		if (trace) {
			tracker.begin();
		}
		InstanceEventProvider<Instance> eventProvider = serviceRegistry.getEventProvider(getInstanceClass());
		int size = 1;
		if (instance != null) {
			loadHeaders(instance, eventProvider, ifMissing);
		} else if (toLoadProps != null) {
			size = toLoadProps.size();
			for (E e : toLoadProps) {
				loadHeaders(e, eventProvider, ifMissing);
			}
		}
		if (trace) {
			LOGGER.trace("Changed event for {} on load handled in : {} ms for {} instance/s",
					getInstanceClass().getSimpleName(), tracker.stop(), size);
		}
	}

	/**
	 * Load headers.
	 *
	 * @param instance
	 *            the instance
	 * @param eventProvider
	 *            the event provider
	 * @param ifMissing
	 *            to generate the headers only if missing
	 */
	protected void loadHeaders(Instance instance, InstanceEventProvider<Instance> eventProvider, boolean ifMissing) {
		if (ifMissing && instance.getProperties().keySet().containsAll(DefaultProperties.DEFAULT_HEADERS)) {
			// nothing to generate all headers are present
			return;
		}
		if (eventProvider != null) {
			InstanceChangeEvent<Instance> event = eventProvider.createChangeEvent(instance);
			if (event != null) {
				eventService.fire(event);
			}
		}
	}

	@Override
	public void saveProperties(Instance instance, boolean addOnly) {
		propertiesService.saveProperties(instance, addOnly);
	}

	@Override
	public <S extends Serializable, T extends Instance> List<T> loadInstancesByDbKey(List<S> ids) {
		return loadInstancesByDbKey(ids, true);
	}

	@Override
	public <S extends Serializable> void delete(Entity<S> entity) {
		if (getInstanceClass().isInstance(entity) || getEntityClass().isInstance(entity)) {
			int deleted = getCache().deleteByKey(entity.getId());
			LOGGER.debug("Deleting {} with id [{}]. Actually removed {}.", entity.getClass().getSimpleName(),
					entity.getId(), deleted);
			if (deleted > 0) {
				deleteProperties(entity);
			}
		}
		// we can throw an exception for just in case
	}

	/**
	 * Delete properties.
	 *
	 * @param <S>
	 *            the generic type
	 * @param entity
	 *            the entity
	 */
	protected <S extends Serializable> void deleteProperties(Entity<S> entity) {
		PathElement path = null;
		boolean supported = false;
		if (getInstanceClass().isInstance(entity)) {
			Instance instance = getInstanceClass().cast(entity);
			path = instance;
			supported = propertiesService.isModelSupported(instance);
		} else if (getEntityClass().isInstance(entity)) {
			Instance convert = toInstance(entity);
			if (convert != null) {
				path = convert;
				supported = propertiesService.isModelSupported(convert);
			}
		}
		if (supported && path != null) {
			propertiesService.removeProperties(entity, path);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends Serializable> Entity<S> saveEntity(Entity<S> entity) {
		if (String.class.equals(getPrimaryIdType())) {
			idManager.generateStringId(entity, true);
		}
		Entity<P> update = null;
		// if entity nothing to do
		if (getEntityClass().isInstance(entity)) {
			update = (Entity<P>) entity;
			// if instance convert the instance
		} else if (getInstanceClass().isInstance(entity)) {
			update = toEntity(getInstanceClass().cast(entity));
		}
		if (update != null) {
			if (!Options.CACHE_ONLY_OPERATION.isEnabled()) {
				update = getDbDao().saveOrUpdate(update);
			}
			getCache().setValue(update.getId(), update);
		} else {
			update = (Entity<P>) entity;
		}
		return (Entity<S>) update;
	}

	/**
	 * {@inheritDoc}<br>
	 * Sets the owned model before return
	 */
	@Override
	public <T extends Instance> List<T> loadInstances(Instance owner, boolean loadProperties) {
		String query = getOwningInstanceQuery();
		// if no query is provided then we cannot do anything so will return now
		if (!idManager.isPersisted(owner) || query == null) {
			// no allowed children on an instance
			return CollectionUtils.emptyList();
		}

		DataTypeDefinition instanceType = dictionaryService.getDataTypeDefinition(getInstanceClass());
		if (instanceType == null) {
			return CollectionUtils.emptyList();
		}
		List<Pair<String, Object>> args = new ArrayList<>(1);
		args.add(new Pair<String, Object>("owningInstanceId", owner.getId()));
		args.add(new Pair<String, Object>("instanceType", instanceType.getId()));
		List<P> list = getDbDao().fetchWithNamed(query, args);
		List<T> loadInstancesByDbKey = loadInstancesByDbKey(list, loadProperties);
		// update the instances model
		for (T t : loadInstancesByDbKey) {
			OwnedModel.setOwningInstance(t, owner);
		}
		return loadInstancesByDbKey;
	}

	/**
	 * Operation should be invoked just before invoking adapter to provide some needed updates on current model
	 * representing the up to date model that would be persisted.
	 *
	 * @param <X>
	 *            the generic type
	 * @param model
	 *            the model
	 * @param fields
	 *            the fields for the model
	 * @return the instance updated
	 */
	@Override
	public <X extends PropertyDefinition> Instance preSaveInstance(PropertyModel model, Collection<X> fields) {
		for (X field : fields) {
			if (model.isPropertyPresent(field.getIdentifier())) {
				continue;
			}
			Serializable serializable = evaluatorManager.evaluate(field,
					evaluatorManager.createDefaultContext((Instance) model, field, null));
			model.addIfNotNull(field.getIdentifier(), serializable);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <I extends Serializable> Class<I> getPrimaryIdType() {
		return (Class<I>) String.class;
	}

	@Override
	public void clearInternalCache() {
		getCache().clear();
	}

	/**
	 * Gets the query that should be used for retrieving entity ID by owning instance reference. The query should have 2
	 * arguments:
	 * <ul>
	 * <li>sourceId - the id for the instance reference identifier
	 * <li>sourceType - the id for the data type definition id
	 * </ul>
	 * If the query is not supported <code>null</code> should be returned
	 *
	 * @return the owning instance query or <code>null</code> if not supported.
	 */
	@SuppressWarnings("static-method")
	protected String getOwningInstanceQuery() {
		return InstanceEntity.QUERY_INSTANCE_ID_BY_INSTANCE_TYPE_AND_OWNING_INSTANCE_KEY;
	}

	/**
	 * Populate instance properties using the given for model.
	 *
	 * @param instance
	 *            the instance to populate
	 * @param model
	 *            the model to use
	 */
	protected abstract void populateInstanceForModel(Instance instance, D model);

	protected <E extends Instance> E loadInstanceInternal(P id, K otherId) {
		E instance = null;
		if (id != null) {
			instance = getInstanceLoader().find(id);
		}
		if (instance == null && otherId != null) {
			instance = getInstanceLoader().findBySecondaryId(otherId);
		}
		if (instance != null) {
			return onInstanceLoad(instance);
		}
		return null;
	}

	/**
	 * Method called on instance load just before to be returned.
	 *
	 * @param instance
	 *            the loaded instance never <code>null</code>.
	 * @return the updated instance to be returned.
	 */
	@SuppressWarnings("static-method")
	protected <E extends Instance> E onInstanceLoad(E instance) {
		return instance;
	}

	protected EntityLookupCache<Serializable, Entity<? extends Serializable>, Serializable> getCache() {
		return getInstanceLoader().getPersistCallback().getCache();
	}

	/**
	 * Convert and save the given instance as entity.<br>
	 * <b>NOTE:</b>The provided implementation handles basic transformation, persisting and cache updating
	 *
	 * @param instance
	 *            the instance
	 * @return the converted entity
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Instance> T convertAndSave(T instance) {
		C entity = toEntity(instance);
		T oldInstance = null;
		// REVIEW: the ID will always be not null so it will generate one additional query for every
		// new instance created
		if (idManager.isPersisted(entity)) {
			// convert and load the old instance
			// NOTE: this will load the children also if we does not require that, something
			// should be changed
			oldInstance = getInstanceLoader().find(instance.getId());
		}
		onBeforeSave(entity, oldInstance);
		if (oldInstance == null) {
			Pair<Serializable, Entity<? extends Serializable>> pair = getCache().getOrCreateByValue(entity);
			if (pair == null) {
				throw new EmfApplicationException("Failed to create instance: " + instance.getClass().getSimpleName());
			}
			if (instance.getId() == null) {
				instance.setId(pair.getFirst());
			}
		} else {
			getCache().updateValue(getInstanceLoader().getPersistCallback().getPrimaryIdLoadHandler().getId(entity),
					entity);
		}
		onAfterSave(instance);
		return oldInstance;
	}

	/**
	 * Converts the given source object to destination class. If the last argument is true then the conversion is done
	 * for the complete tree, otherwise only local copy is performed and relations are not followed.
	 *
	 * @param <S>
	 *            the generic source type
	 * @param <D>
	 *            the generic destination type
	 * @param source
	 *            the source object instance
	 * @param dest
	 *            the destination class type
	 * @return the created and populated object
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Instance> T toInstance(Entity<? extends Serializable> source) {
		return (T) getInstanceLoader().getPersistCallback().getInstanceConverter().convertToInstance(source);
	}

	/**
	 * To entity.
	 *
	 * @param source
	 *            the source
	 * @return the c
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Instance> C toEntity(T source) {
		return (C) getInstanceLoader().getPersistCallback().getEntityConverter().convertToEntity(source);
	}

	/**
	 * After the save this method is called for any additional operations to be executed.
	 *
	 * @param instance
	 *            the instance
	 */
	protected void onAfterSave(Instance instance) {
		// nothing to do here, add the proper implementation in the subclasses
	}

	/**
	 * The method is called just before saving to DB.
	 *
	 * @param entity
	 *            the entity
	 * @param oldCached
	 *            the old cached
	 */
	@SuppressWarnings("unchecked")
	protected void onBeforeSave(C entity, Instance oldCached) {
		if (entity.getId() == null && getPrimaryIdType().equals(String.class)) {
			idManager.generateStringId(entity, true);
		}
	}

	/**
	 * Getter method for dbDao.
	 *
	 * @return the dbDao
	 */
	protected DbDao getDbDao() {
		return dbDao;
	}

	/**
	 * Gets the instance loader used to fetch instances
	 *
	 * @return the instance loader
	 */
	protected InstanceLoader getInstanceLoader() {
		return instanceLoader;
	}

	/**
	 * Gets the instance class for the concrete implementation.
	 *
	 * @return the instance class
	 */
	protected abstract <T extends Instance> Class<T> getInstanceClass();

	/**
	 * Gets the entity class for the concrete implementation.
	 *
	 * @return the entity class
	 */
	protected abstract Class<C> getEntityClass();
}
