package com.sirma.itt.seip.instance.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sirma.itt.seip.Pair;
import com.sirma.itt.seip.convert.TypeConverter;
import com.sirma.itt.seip.db.DbDao;
import com.sirma.itt.seip.domain.instance.ArchivedInstance;
import com.sirma.itt.seip.domain.instance.EmfInstance;
import com.sirma.itt.seip.exception.EmfRuntimeException;
import com.sirma.itt.seip.instance.archive.ArchivedEntity;
import com.sirma.itt.seip.instance.archive.ArchivedInstanceDao;
import com.sirma.itt.seip.mapping.ObjectMapper;

/**
 * Test for {@link VersionDao}.
 *
 * @author A. Kunchev
 */
public class VersionDaoTest {

	@InjectMocks
	private VersionDao dao;

	@Mock
	private DbDao dbDao;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private TypeConverter typeConverter;

	@Mock
	private ArchivedInstanceDao archivedInstanceDao;

	@Before
	public void setup() {
		dao = new VersionDao();
		MockitoAnnotations.initMocks(this);

		when(objectMapper.map(any(ArchivedEntity.class), eq(ArchivedInstance.class)))
				.thenReturn(new ArchivedInstance());
	}

	@Test(expected = EmfRuntimeException.class)
	public void persistVersion_nullInstance() {
		dao.persistVersion(null);
	}

	@Test
	public void persistVersion_persistChangesCalled() {
		EmfInstance instance = new EmfInstance();
		ArchivedInstance archivedInstance = new ArchivedInstance();
		when(typeConverter.convert(ArchivedInstance.class, instance)).thenReturn(archivedInstance);
		dao.persistVersion(instance);
		verify(archivedInstanceDao).persistChanges(archivedInstance);
	}

	@Test
	public void getVersionsCount_notFoundVersions() {
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_COUNT_BY_TARGET_ID_KEY),
				anyListOf(Pair.class))).thenReturn(Collections.emptyList());

		int versionsCount = dao.getVersionsCount("version-instance-id");
		assertEquals(0, versionsCount);
	}

	@Test
	public void getVersionsCount_foundTwoVersions() {
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_COUNT_BY_TARGET_ID_KEY),
				anyListOf(Pair.class))).thenReturn(Arrays.asList(2L));

		int versionsCount = dao.getVersionsCount("version-instance-id");
		assertEquals(2, versionsCount);
	}

	@Test
	public void findVersionsByTargetId_noResultsFound() {
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_BY_REFERENCE_ID_KEY), anyListOf(Pair.class),
				anyInt(), anyInt())).thenReturn(Collections.emptyList());

		Collection<ArchivedInstance> result = dao.findVersionsByTargetId("version-instance-id", 0, -1);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void findVersionsById_withResultsFound() {
		ArchivedEntity archivedEntity = new ArchivedEntity();
		archivedEntity.setCreatedOn(new Date());
		ArchivedEntity archivedEntity1 = new ArchivedEntity();
		archivedEntity1.setCreatedOn(new Date());
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_BY_REFERENCE_ID_KEY), anyListOf(Pair.class),
				anyInt(), anyInt())).thenReturn(Arrays.asList(archivedEntity, archivedEntity1));

		Collection<ArchivedInstance> result = dao.findVersionsByTargetId("version-instance-id", 0, -1);
		assertEquals(2, result.size());
	}

	@Test
	public void findVersionById_noResultFound() {
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_BY_ID_KEY), anyListOf(Pair.class)))
				.thenReturn(Collections.emptyList());

		assertFalse(dao.findVersionById("version-instance-id").isPresent());
	}

	@Test
	public void findVersionById_withResultFound() {
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_BY_ID_KEY), anyListOf(Pair.class)))
				.thenReturn(Arrays.asList(new ArchivedEntity()));

		assertTrue(dao.findVersionById("version-instance-id").isPresent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_nullIdCollection() {
		Map<Serializable, Serializable> resultMap = dao.findVersionIdsByTargetIdAndDate(null, new Date());
		assertEquals(Collections.emptyMap(), resultMap);
		verify(dbDao, never()).fetchWithNamed(anyString(), anyListOf(Pair.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_emptyIdCollection() {
		Map<Serializable, Serializable> resultMap = dao.findVersionIdsByTargetIdAndDate(new ArrayList<>(), new Date());
		assertEquals(Collections.emptyMap(), resultMap);
		verify(dbDao, never()).fetchWithNamed(anyString(), anyListOf(Pair.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_nullDate() {
		Map<Serializable, Serializable> resultMap = dao.findVersionIdsByTargetIdAndDate(new ArrayList<>(), null);
		assertEquals(Collections.emptyMap(), resultMap);
		verify(dbDao, never()).fetchWithNamed(anyString(), anyListOf(Pair.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_nullDateNotEmptyCollection() {
		Map<Serializable, Serializable> resultMap = dao.findVersionIdsByTargetIdAndDate(Arrays.asList("target-id-1"),
				null);
		assertEquals(Collections.emptyMap(), resultMap);
		verify(dbDao, never()).fetchWithNamed(anyString(), anyListOf(Pair.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_nullInputParams() {
		Map<Serializable, Serializable> resultMap = dao.findVersionIdsByTargetIdAndDate(null, null);
		assertEquals(Collections.emptyMap(), resultMap);
		verify(dbDao, never()).fetchWithNamed(anyString(), anyListOf(Pair.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_withResultsFound_oneResultReturned() {
		ArchivedEntity entity1 = new ArchivedEntity();
		entity1.setId("1");
		entity1.setTargetId("targetId-1");
		entity1.setCreatedOn(new Date());

		ArchivedEntity entity2 = new ArchivedEntity();
		entity2.setId("2");
		entity2.setTargetId("targetId-2");

		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_ID_BY_TARGET_ID_AND_CREATED_ON_DATE_KEY),
				anyListOf(Pair.class))).thenReturn(Arrays.asList(entity1, entity2));

		Map<Serializable, Serializable> resultMap = dao
				.findVersionIdsByTargetIdAndDate(Arrays.asList("targetId-1", "targetId-2"), new Date());
		assertEquals(1, resultMap.size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionIdsByTargetIdAndDate_withResultsFound_duplicatedIds() {
		ArchivedEntity entity1 = new ArchivedEntity();
		entity1.setId("1");
		entity1.setTargetId("targetId-1");
		entity1.setCreatedOn(new Date());

		ArchivedEntity entity2 = new ArchivedEntity();
		entity2.setId("1");
		entity2.setTargetId("targetId-1");
		entity2.setCreatedOn(new Date());

		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_ID_BY_TARGET_ID_AND_CREATED_ON_DATE_KEY),
				anyListOf(Pair.class))).thenReturn(Arrays.asList(entity1, entity2));

		Map<Serializable, Serializable> resultMap = dao.findVersionIdsByTargetIdAndDate(Arrays.asList("targetId-1"),
				new Date());
		assertEquals(1, resultMap.size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findVersionsById_foundResults() {
		when(dbDao.fetchWithNamed(eq(ArchivedEntity.QUERY_ARCHIVED_ENTITIES_BY_ID_KEY), anyListOf(Pair.class)))
				.thenReturn(Collections.emptyList());
		assertEquals(dao.findVersionsById(Arrays.asList("version-instance-id", "version-instance-id")).size(), 0);
	}

}