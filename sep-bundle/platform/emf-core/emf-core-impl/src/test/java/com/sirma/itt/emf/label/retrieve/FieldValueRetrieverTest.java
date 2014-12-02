package com.sirma.itt.emf.label.retrieve;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sirma.itt.commons.utils.reflection.ReflectionUtils;
import com.sirma.itt.emf.domain.Pair;

/**
 * Test the {@link FieldValueRetrieverService}.
 * 
 * @author nvelkov
 */
public class FieldValueRetrieverTest {

	private FieldValueRetrieverTestHelper testHelper = new FieldValueRetrieverTestHelper();
	private FieldValueRetrieverServiceImpl fieldValueRetrieverService;

	/**
	 * Re-initializes the {@link FieldValueRetrieverService}.
	 */
	@BeforeMethod
	public void beforeMethod() {
		fieldValueRetrieverService = new FieldValueRetrieverServiceImpl();
	}

	/**
	 * Test the username retriever. In this scenario the username is passed and the user display
	 * name is succesfully returned.
	 */
	@Test
	public void testUsernameRetrieverSuccess() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValueRetrieverInstance(true));
		String user = fieldValueRetrieverService.getLabel(FieldId.USERNAME, "something");
		Assert.assertEquals(user, "mockedUser");
	}

	/**
	 * Test the username retriever when a user display name has not been found. In that case the
	 * retriever should return the original value.
	 */
	@Test
	public void testUsernameRetrieverNotFound() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValueRetrieverInstance(false));
		String user = fieldValueRetrieverService.getLabel(FieldId.USERNAME, "something");
		Assert.assertEquals(user, "something");
	}

	/**
	 * Test the username retriever when a null has been passed to the retriever . In that case the
	 * retriever should return null.
	 */
	@Test
	public void testUsernameRetrieverNull() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValueRetrieverInstance(false));
		String user = fieldValueRetrieverService.getLabel(FieldId.USERNAME, null);
		Assert.assertEquals(user, null);
	}

	/**
	 * Test username values retriever when no filter is applied.
	 */
	@Test
	public void testUsernameValuesRetrieverNoFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.USERNAME, null, 0,
				10);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 10);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "0");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "User 0");
	}

	/**
	 * Test username values retriever with applied filter. Filter must work with starts with
	 * comparison and should be case insenstive.
	 */
	@Test
	public void testUsernameValuesRetrieverWithFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.USERNAME, "uSeR", 0,
				10);
		Assert.assertEquals(values.getTotal(), new Long(10));
		Assert.assertEquals(values.getResults().size(), 10);
		for (Pair<String, String> value : values.getResults()) {
			Assert.assertTrue(value.getSecond().toLowerCase().startsWith("user"));
		}
	}

	/**
	 * Test username values retriever paging. Testing that offset and limit parameters are properly
	 * applied.
	 */
	@Test
	public void testUsernameValuesRetrieverPaging() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.USERNAME, null, 10,
				5);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 5);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "5");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "User 5");
	}

	/**
	 * Test username values retriever with null values for parameters to ensure that method works
	 * with default values.
	 */
	@Test
	public void testUsernameValuesRetrieverNullParams() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockUsernameFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.USERNAME, null,
				null, null);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 20);
	}

	/**
	 * Test the object type retriever. In this scenario the object type is passed and the object
	 * type label is succesfully returned.
	 */
	@Test
	public void testObjectTypeRetrieverSuccess() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValueRetrieverInstance(true));
		String user = fieldValueRetrieverService.getLabel(FieldId.OBJECTTYPE, "something");
		Assert.assertEquals(user, "mockedLabel");
	}

	/**
	 * Test the object type retriever when an object type label has not been found. In that case the
	 * retriever should return the original value.
	 */
	@Test
	public void testObjectTypeRetrieverNotFound() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValueRetrieverInstance(false));
		String user = fieldValueRetrieverService.getLabel(FieldId.OBJECTTYPE, "something");
		Assert.assertEquals(user, "something");
	}

	/**
	 * Test the object type retriever when a null has been passed to the retriever . In that case
	 * the retriever should return null.
	 */
	@Test
	public void testObjectTypeRetrieverNull() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValueRetrieverInstance(false));
		String user = fieldValueRetrieverService.getLabel(FieldId.OBJECTTYPE, null);
		Assert.assertEquals(user, null);
	}

	/**
	 * Test object type values retriever when no filter is applied.
	 */
	@Test
	public void testObjectTypeValuesRetrieverNoFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTTYPE, null, 0,
				10);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 10);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "instance:0");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "Object type 0");
	}

	/**
	 * Test object type values retriever with applied filter. Filter must work with starts with
	 * comparison and should be case insenstive.
	 */
	@Test
	public void testObjectTypeValuesRetrieverWithFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTTYPE,
				"Object Type 1", 0, 10);
		Assert.assertEquals(values.getTotal(), new Long(11));
		Assert.assertEquals(values.getResults().size(), 10);
		for (Pair<String, String> value : values.getResults()) {
			Assert.assertTrue(value.getSecond().toLowerCase().startsWith("object type 1"));
		}
	}

	/**
	 * Test object type values retriever paging. Testing that offset and limit parameters are
	 * properly applied.
	 */
	@Test
	public void testObjectTypeValuesRetrieverPaging() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTTYPE, null,
				10, 5);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 5);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "instance:10");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "Object type 10");
	}

	/**
	 * Test object type values retriever with null values for parameters to ensure that method works
	 * with default values.
	 */
	@Test
	public void testObjectTypeValuesRetrieverNullParams() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectTypeFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTTYPE, null,
				null, null);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 20);
	}

	/**
	 * Test action values retriever. In this scenario the action label is succesfully found and
	 * retrieved.
	 */
	@Test
	public void testActionValuesRetriever() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockActionFieldValueRetrieverInstance(true));
		String label = fieldValueRetrieverService.getLabel(FieldId.ACTIONID, "something");
		Assert.assertEquals(label, "mockedLabel");
	}

	/**
	 * Test action values retriever when the action label is not found so the original value is
	 * returned.
	 */
	@Test
	public void testActionValuesRetrieverNotFound() {

		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockActionFieldValueRetrieverInstance(false));
		String label = fieldValueRetrieverService.getLabel(FieldId.ACTIONID, "something");
		Assert.assertEquals(label, "something");
	}

	/**
	 * Test action values retriever when no filter is applied.
	 */
	@Test
	public void testActionValuesRetrieverNoFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockActionFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.ACTIONID, null, 0,
				10);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 10);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "cv0");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "Label 0");
	}

	/**
	 * Test action values retriever with applied filter. Filter must work with starts with
	 * comparison and should be case insenstive.
	 */
	@Test
	public void testActionValuesRetrieverWithFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockActionFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.ACTIONID, "Label 1",
				0, 10);
		Assert.assertEquals(values.getTotal(), new Long(11));
		Assert.assertEquals(values.getResults().size(), 10);
		for (Pair<String, String> value : values.getResults()) {
			Assert.assertTrue(value.getSecond().toLowerCase().startsWith("label 1"));
		}
	}

	/**
	 * Test action values retriever paging. Testing that offset and limit parameters are properly
	 * applied.
	 */
	@Test
	public void testActionValuesRetrieverPaging() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockActionFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.ACTIONID, null, 10,
				5);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 5);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "cv10");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "Label 10");
	}

	/**
	 * Test action values retriever with null values for parameters to ensure that method works with
	 * default values.
	 */
	@Test
	public void testActionValuesRetrieverNullParams() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockActionFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.ACTIONID, null,
				null, null);
		Assert.assertEquals(values.getTotal(), new Long(20));
		Assert.assertEquals(values.getResults().size(), 20);
	}

	/**
	 * Test object state retriever. In this scenario the label is retrieved succesfully from the
	 * mocked codelist.
	 */
	@Test
	public void testObjectStateRetriever() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectStateFieldValueRetrieverInstance("someInstance", true, true));
		String label = fieldValueRetrieverService.getLabel(FieldId.OBJECTSTATE, "something",
				"somethin");
		Assert.assertEquals(label, "mockedLabel");
	}

	/**
	 * Test domain object state retriever. In this scenario the label is retriever succesfully from
	 * the mocked codelist.
	 */
	@Test
	public void testDomainObjectStateRetriever() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping", testHelper
				.mockObjectStateFieldValueRetrieverInstance(
						"com.sirma.itt.objects.domain.model.ObjectInstance", true, true));
		String label = fieldValueRetrieverService.getLabel(FieldId.OBJECTSTATE, "something",
				"somethin");
		Assert.assertEquals(label, "mockedLabel");
	}

	/**
	 * Test object state retriever when the codelist is not found.
	 */
	@Test
	public void testObjectStateRetrieverNotFound() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping", testHelper
				.mockObjectStateFieldValueRetrieverInstance(
						"com.sirma.itt.objects.domain.model.ObjectInstance", false, true));
		String label = fieldValueRetrieverService.getLabel(FieldId.OBJECTSTATE, "something",
				"somethin");
		Assert.assertEquals(label, "something");
	}

	/**
	 * Test object state retriever when the codevalue is not found.
	 */
	@Test
	public void testObjectStateRetrieverCodeListNotFound() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping", testHelper
				.mockObjectStateFieldValueRetrieverInstance(
						"com.sirma.itt.objects.domain.model.ObjectInstance", true, false));
		String label = fieldValueRetrieverService.getLabel(FieldId.OBJECTSTATE, "something",
				"somethin");
		Assert.assertEquals(label, "something");
	}

	/**
	 * Test object state values retriever when no filter is applied.
	 */
	@Test
	public void testObjectStateValuesRetrieverNoFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectStateFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTSTATE, null,
				0, 10);
		Assert.assertEquals(values.getTotal(), new Long(9));
		Assert.assertEquals(values.getResults().size(), 9);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "cv0");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "Label 0");
	}

	/**
	 * Test object state values retriever with applied filter. Filter must work with starts with
	 * comparison and should be case insenstive.
	 */
	@Test
	public void testObjectStateValuesRetrieverWithFilter() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectStateFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTSTATE,
				"Label 1", 0, 10);
		Assert.assertEquals(values.getTotal(), new Long(3));
		Assert.assertEquals(values.getResults().size(), 3);
		for (Pair<String, String> value : values.getResults()) {
			Assert.assertTrue(value.getSecond().toLowerCase().startsWith("label 1"));
		}
	}

	/**
	 * Test object state values retriever paging. Testing that offset and limit parameters are
	 * properly applied.
	 */
	@Test
	public void testObjectStateValuesRetrieverPaging() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectStateFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTSTATE, null,
				5, 5);
		Assert.assertEquals(values.getTotal(), new Long(9));
		Assert.assertEquals(values.getResults().size(), 4);
		Assert.assertEquals(values.getResults().get(0).getFirst(), "cv11");
		Assert.assertEquals(values.getResults().get(0).getSecond(), "Label 11");
	}

	/**
	 * Test object state values retriever with null values for parameters to ensure that method
	 * works with default values.
	 */
	@Test
	public void testObjectStateValuesRetrieverNullParams() {
		ReflectionUtils.setField(fieldValueRetrieverService, "extensionMapping",
				testHelper.mockObjectStateFieldValuesRetrieverInstance());
		RetrieveResponse values = fieldValueRetrieverService.getValues(FieldId.OBJECTSTATE, null,
				null, null);
		Assert.assertEquals(values.getTotal(), new Long(9));
		Assert.assertEquals(values.getResults().size(), 9);
	}

}
