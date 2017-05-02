package com.sirma.itt.emf.label;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sirma.itt.emf.db.EmfQueries;
import com.sirma.itt.seip.Pair;
import com.sirma.itt.seip.annotation.Documentation;
import com.sirma.itt.seip.cache.CacheConfiguration;
import com.sirma.itt.seip.cache.Eviction;
import com.sirma.itt.seip.cache.lookup.EntityLookupCache;
import com.sirma.itt.seip.cache.lookup.EntityLookupCacheContext;
import com.sirma.itt.seip.cache.lookup.EntityLookupCallbackDAOAdaptor;
import com.sirma.itt.seip.collections.CollectionUtils;
import com.sirma.itt.seip.db.DbDao;
import com.sirma.itt.seip.db.exceptions.DatabaseException;
import com.sirma.itt.seip.definition.label.LabelDefinition;
import com.sirma.itt.seip.definition.label.LabelService;
import com.sirma.itt.seip.definition.model.LabelImpl;
import com.sirma.itt.seip.model.SerializableValue;

/**
 * Default implementation for label service
 *
 * @author BBonev
 */
@ApplicationScoped
public class LabelServiceImpl implements LabelService {
	/** The Constant LABEL_CACHE. */
	@CacheConfiguration(eviction = @Eviction(maxEntries = 2000) , doc = @Documentation(""
			+ "Cache used to store the definition label entries. There is an entry for every unique defined label in all definitions. "
			+ "<br>Minimal value expression: labels * 1.2") )
	private static final String LABEL_CACHE = "LABEL_CACHE";
	/** The db dao. */
	@Inject
	private DbDao dbDao;
	/** The label cache. */
	@Inject
	private EntityLookupCacheContext cacheContext;

	/**
	 * Inits the.
	 */
	@PostConstruct
	public void init() {
		if (!cacheContext.containsCache(LABEL_CACHE)) {
			cacheContext.createCache(LABEL_CACHE, new LabelLookup());
		}
	}

	@Override
	public LabelDefinition getLabel(String name) {
		Pair<String, LabelDefinition> pair = getLabelCache().getByKey(name);
		if (pair != null) {
			return pair.getSecond();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(TxType.REQUIRED)
	public boolean saveLabel(LabelDefinition labelDefinition) {
		if (labelDefinition == null) {
			return false;
		}
		Pair<String, LabelDefinition> pair = getLabelCache().getByKey(labelDefinition.getIdentifier());
		if (pair != null) {
			LabelImpl second = (LabelImpl) pair.getSecond();
			second.setLabels(Collections.unmodifiableMap(labelDefinition.getLabels()));
			second.getValue().setSerializable((Serializable) labelDefinition.getLabels());
			dbDao.saveOrUpdate(second);
			return true;
		}
		Map<String, String> labels = labelDefinition.getLabels();
		if (labels instanceof Serializable) {
			LabelImpl impl = (LabelImpl) labelDefinition;
			if (impl.getValue() == null) {
				impl.setValue(new SerializableValue());
			}
			impl.getValue().setSerializable((Serializable) labels);

			impl = dbDao.saveOrUpdate(impl);
			getLabelCache().setValue(impl.getIdentifier(), impl);
			return true;
		}
		return false;
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public boolean saveLabels(List<LabelDefinition> definitions) {
		if (definitions == null || definitions.isEmpty()) {
			return true;
		}
		Map<String, LabelDefinition> labelIds = new LinkedHashMap<>((int) (definitions.size() * 1.1), 0.95f);
		for (LabelDefinition labelDefinition : definitions) {
			labelIds.put(labelDefinition.getIdentifier(), labelDefinition);
		}
		List<Pair<String, Object>> args = new ArrayList<>(1);
		args.add(new Pair<String, Object>("labelId", labelIds.keySet()));
		List<LabelImpl> list = dbDao.fetchWithNamed(EmfQueries.QUERY_LABELS_BY_ID_KEY, args);
		if (list == null) {
			list = CollectionUtils.EMPTY_LIST;
		}

		EntityLookupCache<String, LabelDefinition, Serializable> labelCache = getLabelCache();
		// update the labels that are int the DB
		for (LabelImpl labelImpl : list) {
			LabelDefinition definition = labelIds.remove(labelImpl.getIdentifier());
			if (definition == null) {
				// should not happen or something is very wrong!
				continue;
			}
			labelImpl.setLabels(Collections.unmodifiableMap(definition.getLabels()));
			labelImpl.getValue().setSerializable((Serializable) definition.getLabels());
			dbDao.saveOrUpdate(labelImpl);
			labelCache.setValue(labelImpl.getIdentifier(), labelImpl);
		}

		// persist new labels
		for (LabelDefinition labelDefinition : labelIds.values()) {
			Map<String, String> labels = labelDefinition.getLabels();
			if (labels instanceof Serializable) {
				LabelImpl impl = (LabelImpl) labelDefinition;
				if (impl.getValue() == null) {
					impl.setValue(new SerializableValue());
				}
				impl.getValue().setSerializable((Serializable) labels);

				impl = dbDao.saveOrUpdate(impl);
				getLabelCache().setValue(impl.getIdentifier(), impl);
			}
		}
		return true;
	}

	/**
	 * Getter method for labelCache.
	 *
	 * @return the labelCache
	 */
	protected EntityLookupCache<String, LabelDefinition, Serializable> getLabelCache() {
		return cacheContext.getCache(LABEL_CACHE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearCache() {
		getLabelCache().clear();
	}

	/**
	 * The Class LabelLookup.
	 *
	 * @author BBonev
	 */
	protected class LabelLookup extends EntityLookupCallbackDAOAdaptor<String, LabelDefinition, Serializable> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Pair<String, LabelDefinition> findByKey(String key) {
			List<LabelImpl> list = dbDao.fetchWithNamed(EmfQueries.QUERY_LABEL_BY_ID_KEY,
					Arrays.asList(new Pair<String, Object>("labelId", key)));
			if (list.isEmpty()) {
				return null;
			}
			if (list.size() > 1) {
				throw new DatabaseException("More then one record found for label: " + key);
			}
			LabelImpl impl = list.get(0);
			SerializableValue serializableValue = impl.getValue();
			Serializable serializable = null;
			if (serializableValue != null && (serializable = serializableValue.getSerializable()) != null) {
				if (serializable instanceof Map) {
					Map<String, String> labels = (Map<String, String>) serializable;
					impl.setLabels(Collections.unmodifiableMap(labels));
				}
			}
			return new Pair<String, LabelDefinition>(key, impl);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pair<String, LabelDefinition> createValue(LabelDefinition value) {
			throw new UnsupportedOperationException("Labels are persisted externaly");
		}

	}

}
