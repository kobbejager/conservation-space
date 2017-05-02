package com.sirma.itt.emf.semantic.persistence;

import static com.sirma.itt.seip.domain.instance.DefaultProperties.SEMANTIC_TYPE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;

import javax.ejb.EJBException;

import org.mockito.Mockito;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.GeneralSemanticTest;
import com.sirma.itt.emf.mocks.DictionaryServiceMock;
import com.sirma.itt.emf.mocks.NamespaceRegistryMock;
import com.sirma.itt.emf.mocks.SemanticDefinitionServiceMock;
import com.sirma.itt.emf.mocks.TransactionalRepositoryConnectionMock;
import com.sirma.itt.emf.mocks.search.QueryBuilderMock;
import com.sirma.itt.emf.mocks.search.SemanticPropertiesWriteConverterMock;
import com.sirma.itt.seip.convert.TypeConverter;
import com.sirma.itt.seip.db.DbDao;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.InstanceReference;
import com.sirma.itt.seip.domain.instance.OwnedModel;
import com.sirma.itt.seip.instance.InstanceTypes;
import com.sirma.itt.seip.instance.ObjectInstance;
import com.sirma.itt.seip.resources.EmfUser;
import com.sirma.itt.seip.security.context.SecurityContext;
import com.sirma.itt.seip.testutil.fakes.InstanceTypeFake;
import com.sirma.itt.seip.testutil.mocks.InstanceProxyMock;
import com.sirma.itt.seip.testutil.mocks.InstanceReferenceMock;
import com.sirma.itt.semantic.NamespaceRegistryService;
import com.sirma.itt.semantic.TransactionalRepositoryConnection;
import com.sirma.itt.semantic.model.vocabulary.EMF;
import com.sirma.itt.semantic.model.vocabulary.Proton;
import com.sirma.itt.semantic.queries.QueryBuilder;

/**
 * Test for {@link SemanticDbDaoImpl}
 *
 * @author Valeri Tishev
 */
public class SemanticDaoTest extends GeneralSemanticTest<DbDao> {

	final String expectedSubInstancePropertyName = "subInstancePropertyName";

	final String expectedStringPropertyName = "emf:stringPropertyName";
	final String expectedStringPropertyValue = "sample string property value";

	final String expectedBooleanPropertyName = "booleanPropertyName";
	final Boolean expectedBooleanPropertyValue = Boolean.TRUE;

	private final DbDao cut = new SemanticDbDaoImpl();
	private NamespaceRegistryService namespaceRegistryService;

	final String expectedOwningInstanceId = "emf:my-case";
	private TransactionalRepositoryConnection transactionalRepositoryConnection;

	/**
	 * Initialize class under test.
	 */
	@BeforeClass
	public void initializeClassUnderTest() {
		namespaceRegistryService = new NamespaceRegistryMock(context);
		ReflectionUtils.setField(cut, "namespaceRegistryService", namespaceRegistryService);
		ReflectionUtils.setField(cut, "dictionaryService", new DictionaryServiceMock());
		ReflectionUtils.setField(cut, "valueFactory", new ValueFactoryImpl());

		ReflectionUtils.setField(cut, "idManager", idManager);

		QueryBuilder queryBuilder = new QueryBuilderMock(context);
		ReflectionUtils.setField(cut, "queryBuilder", queryBuilder);

		SemanticPropertiesReadConverter readConverter = new SemanticPropertiesReadConverter();
		ReflectionUtils.setField(readConverter, "namespaceRegistryService", namespaceRegistryService);

		SemanticDefinitionServiceMock semanticDefinitionService = new SemanticDefinitionServiceMock(context);
		SemanticPropertiesWriteConverter writeConverter = new SemanticPropertiesWriteConverterMock(context);

		ReflectionUtils.setField(cut, "readConverter", readConverter);
		ReflectionUtils.setField(cut, "writeConverter", writeConverter);

		ReflectionUtils.setField(cut, "semanticDefinitionService", semanticDefinitionService);
		InstanceTypes instanceTypes = mock(InstanceTypes.class);
		when(instanceTypes.from(any(Instance.class))).then(a -> Optional.ofNullable(semanticDefinitionService
						.getClassInstance(a.getArgumentAt(0, Instance.class).getAsString(SEMANTIC_TYPE)))
					.map(Instance::type));
		ReflectionUtils.setField(cut, "instanceTypes", instanceTypes);

		// mock the type converter
		InstanceReference instanceReferenceMock = mockInstanceReference();
		TypeConverter typeConverterMock = Mockito.mock(TypeConverter.class);
		Mockito.when(typeConverterMock.convert(InstanceReference.class, EMF.DOMAIN_OBJECT)).thenReturn(
				instanceReferenceMock);

		Mockito.when(typeConverterMock.convert(Class.class, ObjectInstance.class.getName())).thenReturn(
				ObjectInstance.class);

		SecurityContext securityCtx = mock(SecurityContext.class);
		EmfUser user = new EmfUser();
		user.setId("emf:admin");
		when(securityCtx.getAuthenticated()).thenReturn(user);
		ReflectionUtils.setField(cut, "securityContext", securityCtx);
	}

	@Override
	@BeforeMethod
	public void beforeMethod() {
		super.beforeMethod();
		transactionalRepositoryConnection = new TransactionalRepositoryConnectionMock(context);

		ReflectionUtils.setField(cut, "repositoryConnection",
				new InstanceProxyMock<>(transactionalRepositoryConnection));

		try {
			transactionalRepositoryConnection.afterBegin();
		} catch (EJBException | RemoteException e) {
			fail("", e);
		}
	}

	@AfterMethod
	public void commitTransaction() {
		try {
			transactionalRepositoryConnection.beforeCompletion();
			transactionalRepositoryConnection.afterCompletion(true);
		} catch (EJBException | RemoteException e) {
			fail("transaction commit failed", e);
		}
	}

	/**
	 * Test saving new case instance.
	 *
	 * @throws RepositoryException
	 *             the repository exception
	 * @throws MalformedQueryException
	 *             the malformed query exception
	 * @throws QueryEvaluationException
	 *             the query evaluation exception
	 */
	@Test
	public void testSavingNewCaseInstance() throws RepositoryException, MalformedQueryException,
			QueryEvaluationException, EJBException, RemoteException {
		Instance caseInstance = createInstance(expectedStringPropertyValue, expectedBooleanPropertyValue);

		// save the case instance via the semantic DAO
		cut.saveOrUpdate(caseInstance);

		commitTransaction();

		// assert that the DAO has assigned id of the
		// newly created instance object
		Assert.assertNotNull(caseInstance.getId());

		// ask the semantic repository
		// whether there is any instance
		// which is part of the expected parent instance
		RepositoryConnection repositoryConnection = getConnectionFactory().produceConnection();
		StringBuilder query = new StringBuilder();
		query
				.append(namespaceRegistryService.getNamespaces())
					.append("ask {")
					.append(caseInstance.getId().toString())
					.append(" ")
					.append(Proton.PREFIX)
					.append(":")
					.append("partOf")
					.append(" ")
					.append(expectedOwningInstanceId)
					.append("}");

		BooleanQuery booleanQuery = repositoryConnection.prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
		Assert.assertTrue(booleanQuery.evaluate());

		// clear the query builder
		query = new StringBuilder();

		// ask the semantic repository
		// whether the newly created case instance
		// has a stringPropertyName with the
		// expected stringPropertyValue
		query
				.append(namespaceRegistryService.getNamespaces())
					.append("ask {")
					.append(caseInstance.getId().toString())
					.append(" ")
					.append(expectedStringPropertyName)
					.append(" ")
					.append("?value.")
					// .append("'")
					// .append(expectedStringPropertyValue)
					// .append("'")
					.append("}");

		booleanQuery = repositoryConnection.prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
		Assert.assertTrue(booleanQuery.evaluate());
		getConnectionFactory().disposeConnection(repositoryConnection);
	}

	private Instance createInstance(Serializable property1Value, Serializable property2Value) {
		// create a new case instance
		Instance caseInstance = new ObjectInstance();
		// set its case definition
		// see test/resources/definitions/genericCaseDev.xml
		caseInstance.setIdentifier("genericCaseDev");
		InstanceTypeFake.setType(caseInstance, EMF.CASE.toString(), "caseinstance");

		// set instance's properties map
		caseInstance.add(expectedStringPropertyName, property1Value);
		caseInstance.add(expectedBooleanPropertyName, property2Value);
		// caseInstance.add(expectedSubInstancePropertyName, "emf:link-to");
		// caseInstance.add("subInstanceFromRef", new LinkSourceId("emf:refId", mock(DataTypeDefinition.class)));
		// ObjectInstance instance = new ObjectInstance();
		// instance.getOrCreateProperties();
		// instance.setIdentifier("genericCaseDev");
		// instance.setId("emf:instId");
		// InstanceTypeFake.setType(instance, EMF.CASE.toString(), "caseinstance");
		// caseInstance.add("subInstanceFromInst", instance);
		caseInstance.add("uriField", "emf:link-to");
		caseInstance.add("userField", "emf:link-to");
		// caseInstance.add("uriFieldFromRef", new LinkSourceId("emf:refId", mock(DataTypeDefinition.class)));
		// ObjectInstance = instance = new ObjectInstance();
		// instance.getOrCreateProperties();
		// instance.setIdentifier("genericCaseDev");
		// instance.setId("emf:instId");
		// InstanceTypeFake.setType(instance, EMF.CASE.toString(), "caseinstance");
		// caseInstance.add("uriFieldFromInst", instance);
		caseInstance.add("multiUserField", (Serializable) Arrays.asList("emf:user1", "emf:user2"));
		caseInstance.add("multiValueField", (Serializable) Arrays.asList("value1", "value2"));
		caseInstance.add("emf:multiValueField", (Serializable) Arrays.asList("value1", "value2", null));
		// instance = new ObjectInstance();
		// instance.getOrCreateProperties();
		// instance.setIdentifier("genericCaseDev");
		// instance.setId("emf:instId");
		// InstanceTypeFake.setType(instance, EMF.CASE.toString(), "caseinstance");
		// caseInstance.add("emf:uriFieldFromInst", instance);

		// BaseStringIdEntity entity = new BaseStringIdEntity();
		// entity.setId("emf:someEntity");
		// caseInstance.add("subInstanceFromEntity", entity);

		// create and set owning instance of the case

		ObjectInstance project = new ObjectInstance();
		project.setId(expectedOwningInstanceId);
		InstanceTypeFake.setType(project, EMF.PROJECT.toString(), "projectinstance");
		((OwnedModel) caseInstance).setOwningInstance(project);
		return caseInstance;
	}

	@Test
	public void test_saveInstanceDiff() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		Instance instance = createInstance("some value that should be updated", Boolean.FALSE);

		// save the instance that will be updated later
		Instance updated = cut.saveOrUpdate(instance, null);

		Instance newInstance = createInstance(expectedStringPropertyValue, expectedBooleanPropertyValue);
		newInstance.getProperties().remove(expectedSubInstancePropertyName);
		newInstance.setId(updated.getId());

		cut.saveOrUpdate(newInstance, updated);

		commitTransaction();

		RepositoryConnection repositoryConnection = getConnectionFactory().produceConnection();
		StringBuilder query = new StringBuilder();

		// ask the semantic repository
		// whether the updated created case instance
		// has a stringPropertyName with the
		// expected stringPropertyValue
		query
				.append(namespaceRegistryService.getNamespaces())
					.append("ask {")
					.append(updated.getId().toString())
					.append(" ")
					.append(expectedStringPropertyName)
					.append(" ")
					.append("?value.")
					// .append("'")
					// .append(expectedStringPropertyValue)
					// .append("'")
					.append("}");

		BooleanQuery booleanQuery = repositoryConnection.prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
		Assert.assertTrue(booleanQuery.evaluate());
		getConnectionFactory().disposeConnection(repositoryConnection);
	}

	@Test
	public void test_fetchById() {
		// see test/resources/data/SemanticDaoTestData.ttl
		Instance found = cut.find(Instance.class, "emf:my_case");
		assertNotNull(found);
		assertEquals(found.getId(), "emf:my_case");
		assertEquals(found.getString("stringPropertyName"), "sample case string property");
		assertEquals(found.getBoolean(expectedBooleanPropertyName), expectedBooleanPropertyValue.booleanValue());
	}

	/**
	 * Test find instance by id.
	 */
	@Test
	public void testFindInstanceById() {
		// see test/resources/data/SemanticDaoTestData.ttl
		final String expectedCaseSectionId = "emf:my-section";
		ObjectInstance found = cut.find(ObjectInstance.class, expectedCaseSectionId);

		Assert.assertNotNull(found);
		// see test/resources/data/SemanticDaoTestData.ttl
		Assert.assertEquals("dms_ID", found.getDmsId());
	}

	@Test
	public void test_delete() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		Instance instance = createInstance("some value that should be deleted", Boolean.FALSE);

		// save the instance that will be updated later
		Instance updated = cut.saveOrUpdate(instance, null);

		commitTransaction();

		Instance found = cut.find(Instance.class, updated.getId());
		assertNotNull(found);
		assertEquals(found.getId(), updated.getId());

		cut.delete(ObjectInstance.class, updated.getId());

		found = cut.find(Instance.class, updated.getId());
		assertNull(found);
	}

	@Test
	public void test_deleteMultiple() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		Instance instance1 = createInstance("some value that should be deleted 1", Boolean.FALSE);
		Instance instance2 = createInstance("some value that should be deleted 2", Boolean.TRUE);

		// save the instance that will be updated later
		Instance updated1 = cut.saveOrUpdate(instance1);
		Instance updated2 = cut.saveOrUpdate(instance2);

		commitTransaction();

		Instance found1 = cut.find(Instance.class, updated1.getId());
		assertNotNull(found1);
		assertEquals(found1.getId(), updated1.getId());
		Instance found2 = cut.find(Instance.class, updated2.getId());
		assertNotNull(found2);
		assertEquals(found2.getId(), updated2.getId());

		cut.delete(Instance.class, (Serializable) Arrays.asList(updated1.getId(), updated2.getId()));

		found1 = cut.find(Instance.class, updated1.getId());
		assertNull(found1);
		found2 = cut.find(Instance.class, updated2.getId());
		assertNull(found2);
	}

	@Override
	protected String getTestDataFile() {
		return "SemanticDaoTestData.ttl";
	}

	/**
	 * Mock instance reference.
	 *
	 * @return the instance reference
	 */
	private static InstanceReference mockInstanceReference() {
		return new InstanceReferenceMock();
	}

}
