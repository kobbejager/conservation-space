/**
 *
 */
package com.sirma.itt.seip.instance;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.definition.model.PropertyDefinitionProxy;
import com.sirma.itt.seip.definition.model.RegionDefinitionImpl;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.PropertyDefinition;
import com.sirma.itt.seip.domain.definition.label.LabelProvider;
import com.sirma.itt.seip.domain.instance.EmfInstance;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.expressions.ExpressionContext;
import com.sirma.itt.seip.expressions.ExpressionsManager;
import com.sirma.itt.seip.instance.dao.InstanceDao;
import com.sirma.itt.seip.instance.dao.ServiceRegistry;
import com.sirma.itt.seip.instance.properties.PropertiesService;
import com.sirma.itt.seip.testutil.EmfTest;
import com.sirma.itt.seip.testutil.mocks.DefinitionMock;

/**
 * @author BBonev
 *
 */
@Test
public class HeadersServiceTest extends EmfTest {
	@InjectMocks
	HeadersService headersService;

	@Mock
	private ExpressionsManager expressionsManager;
	@Mock
	private DictionaryService dictionaryService;
	@Mock
	private LabelProvider labelProvider;
	@Mock
	private PropertiesService propertiesService;
	@Mock
	private ServiceRegistry serviceRegistry;

	@Override
	@BeforeMethod
	public void beforeMethod() {
		super.beforeMethod();
		for (int i = 0; i < 7; i++) {
			when(labelProvider.getLabel("label" + i, userPreferences.getLanguage())).thenReturn("Header " + i);
		}
		when(expressionsManager.evaluateRule(anyString(), eq(String.class), any(ExpressionContext.class),
				any(Instance.class))).then(a -> a.getArgumentAt(0, String.class));
	}

	@Test
	public void test_generateSingleHeader_invalid() {
		assertNull(headersService.generateInstanceHeader(null, null));
		// no instance
		assertNull(headersService.generateInstanceHeader(null, "header6"));

		Instance instance = new EmfInstance();
		instance.setId("emf:instance1");

		// no properties
		assertNull(headersService.generateInstanceHeader(instance, "header6"));

		// normally the instance should have loaded properties
		instance.add("property", "Some value");

		// no header
		assertNull(headersService.generateInstanceHeader(instance, null));

		// no definition
		assertNull(headersService.generateInstanceHeader(instance, "header6"));
	}

	@Test
	public void test_generateSingleHeader() {
		Instance instance = new EmfInstance();
		instance.setId("emf:instance1");
		// normally the instance should have loaded properties
		instance.add("property", "Some value");
		when(dictionaryService.getInstanceDefinition(instance)).thenReturn(createDefinitionMock());

		// generate header that is located in region also
		String header = headersService.generateInstanceHeader(instance, "header6");

		assertEquals(header, "Header 6");
	}

	@Test
	public void test_generateInstanceHeaders_invalidData() {
		Instance instance = new EmfInstance();
		instance.setId("emf:instance1");

		// no properties
		headersService.generateInstanceHeaders(instance, false, "header1");
		assertFalse(instance.isPropertyPresent("header1"));

		// normally the instance should have loaded properties
		instance.add("property", "Some value");

		// no headers
		headersService.generateInstanceHeaders(instance, false, (String[]) null);
		assertTrue(instance.getProperties().size() == 1);
		headersService.generateInstanceHeaders(instance, false, new String[0]);
		assertTrue(instance.getProperties().size() == 1);

		// no model
		headersService.generateInstanceHeaders(instance, false, "header1");
		assertFalse(instance.isPropertyPresent("header1"));
	}

	@Test
	public void test_generateInstanceHeaders_noSave() {
		Instance instance = new EmfInstance();
		instance.setId("emf:instance1");
		// normally the instance should have loaded properties
		instance.add("property", "Some value");
		when(dictionaryService.getInstanceDefinition(instance)).thenReturn(createDefinitionMock());

		// generate header that is located in region also
		headersService.generateInstanceHeaders(instance, false, "header1", "header2", "header6");

		assertTrue(instance.isPropertyPresent("header1"));
		assertTrue(instance.isPropertyPresent("header2"));
		assertTrue(instance.isPropertyPresent("header6"));

		assertEquals(instance.getString("header1"), "Header 1");
		assertEquals(instance.getString("header2"), "Header 2");
		assertEquals(instance.getString("header6"), "Header 6");

	}

	@Test
	public void test_generateInstanceHeaders_save_viaDao() {
		Instance instance = new EmfInstance();
		instance.setId("emf:instance1");
		// normally the instance should have loaded properties
		instance.add("property", "Some value");
		when(dictionaryService.getInstanceDefinition(instance)).thenReturn(createDefinitionMock());

		InstanceDao dao = mock(InstanceDao.class);
		when(serviceRegistry.getInstanceDao(instance)).thenReturn(dao);
		when(propertiesService.isModelSupported(instance)).thenReturn(false);

		headersService.generateInstanceHeaders(instance, true, "header1", "header2", "header6");

		assertTrue(instance.isPropertyPresent("header1"));
		assertTrue(instance.isPropertyPresent("header2"));
		assertTrue(instance.isPropertyPresent("header6"));

		assertEquals(instance.getString("header1"), "Header 1");
		assertEquals(instance.getString("header2"), "Header 2");
		assertEquals(instance.getString("header6"), "Header 6");

		verify(dao).saveProperties(instance, false);
	}

	@Test
	public void test_generateInstanceHeaders_save_viaPropService() {
		Instance instance = new EmfInstance();
		instance.setId("emf:instance1");
		instance.setRevision(1L);
		// normally the instance should have loaded properties
		instance.add("property", "Some value");
		when(dictionaryService.getInstanceDefinition(instance)).thenReturn(createDefinitionMock());

		InstanceDao dao = mock(InstanceDao.class);
		when(serviceRegistry.getInstanceDao(instance)).thenReturn(dao);
		when(propertiesService.isModelSupported(instance)).thenReturn(true);

		headersService.generateInstanceHeaders(instance, true, "header1", "header2", "header6");

		assertTrue(instance.isPropertyPresent("header1"));
		assertTrue(instance.isPropertyPresent("header2"));
		assertTrue(instance.isPropertyPresent("header6"));

		assertEquals(instance.getString("header1"), "Header 1");
		assertEquals(instance.getString("header2"), "Header 2");
		assertEquals(instance.getString("header6"), "Header 6");

		verify(dao, never()).saveProperties(instance, false);

		HashMap<String, Serializable> generatedHeaders = new HashMap<>(instance.getProperties());
		generatedHeaders.remove("property");

		verify(propertiesService).saveProperties(eq(instance), eq(instance), eq(generatedHeaders), eq(true));
	}

	static DefinitionModel createDefinitionMock() {
		DefinitionMock mock = new DefinitionMock();
		mock.getFields().add(buildField("header1", "label1"));
		mock.getFields().add(buildField("header2", "label2"));
		mock.getFields().add(buildField("header3", "label3"));
		mock.getFields().add(buildField("header4", null));
		mock.getFields().add(buildField("", "label5"));

		RegionDefinitionImpl region = new RegionDefinitionImpl();
		region.getFields().add(buildField("header6", "label6"));
		mock.getRegions().add(region);

		return mock;
	}

	private static PropertyDefinition buildField(String name, String labelId) {
		PropertyDefinitionProxy field = new PropertyDefinitionProxy();
		field.setIdentifier(name);
		field.setLabelId(labelId);
		return field;
	}
}
