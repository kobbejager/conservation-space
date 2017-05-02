package com.sirma.itt.seip.export.renders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sirma.itt.seip.configuration.SystemConfiguration;
import com.sirma.itt.seip.convert.TypeConverter;
import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.domain.codelist.CodelistService;
import com.sirma.itt.seip.domain.definition.DataTypeDefinition;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.label.LabelProvider;
import com.sirma.itt.seip.domain.instance.DefaultProperties;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.search.SearchArguments;
import com.sirma.itt.seip.domain.search.SearchRequest;
import com.sirma.itt.seip.domain.search.Sorter;
import com.sirma.itt.seip.export.renders.utils.JsoupUtil;
import com.sirma.itt.seip.expressions.conditions.ConditionsManager;
import com.sirma.itt.seip.instance.InstanceTypeResolver;
import com.sirma.itt.seip.instance.dao.InstanceLoadDecorator;
import com.sirma.itt.seip.instance.util.LinkProviderService;
import com.sirma.itt.seip.search.SearchService;
import com.sirma.itt.seip.search.converters.JsonToConditionConverter;
import com.sirma.itt.seip.search.converters.JsonToDateRangeConverter;
import com.sirma.itt.seip.testutil.mocks.ConfigurationPropertyMock;
import com.sirma.itt.seip.testutil.mocks.PropertyDefinitionMock;
import com.sirmaenterprise.sep.content.idoc.ContentNodeFactory;
import com.sirmaenterprise.sep.content.idoc.Idoc;
import com.sirmaenterprise.sep.content.idoc.Widget;
import com.sirmaenterprise.sep.content.idoc.WidgetConfiguration;
import com.sirmaenterprise.sep.content.idoc.nodes.WidgetNode;
import com.sirmaenterprise.sep.content.idoc.nodes.layout.LayoutManagerBuilder;
import com.sirmaenterprise.sep.content.idoc.nodes.layout.LayoutNodeBuilder;

/**
 * Test class for DataTableWidget.
 * 
 * @author Hristo Lungov
 */
public class DataTableWidgetTest {

	private static final String UI2_URL = "https://ses.sirmaplatform.com/#/idoc/";
	private static final String RESULTS = "2Results";
	private static final int CL_210 = 210;
	private static final String INSTANCE_ONE_TITLE = "new configuration DTW";
	private static final String INSTANCE_TWO_TITLE = "tests";
	private static final String CL210_TITLE_DESCRIPTION = "Common document";
	private static final String INSTANCE_TWO_ID = "emf:f3908df1-6160-49cd-ba70-840866019785";
	private static final String INSTANCE_ONE_ID = "emf:76dcea11-ee66-45ae-8d5a-f2c42afc67ce";
	private static final String TABLE_TITLE = "DTW Title";
	private static final String ENTITY = "Entity";
	private static final String srcOne = "image src one";
	private static final String hrefOne = "instance-one-href";
	private static final String labelOne = "Instance one label";
	private static final String srcTwo = "image src two";
	private static final String hrefTwo = "instance-two-href";
	private static final String labelTwo = "Instance two label";

	private static final String HYPERLINK_LABEL = "(Common document) barnat.pdf";
	private static final String TEST_TITLE = "Test Title";
	private static final String INSTANCE_ID = "instanceId";
	private static final String TITLE = "Title";
	private static final String TYPE = "Type";
	private static final String DATA_TABLE_WIDGET_HTML_FILE = "data-table-widget.html";
	private static final String DATA_TABLE_WIDGET_ID = "0dda955c-e9cd-47e7-be51-ec5ccf66de5a";
	private static final String DATA_TABLE_WIDGET_CURRENT_OBJECT_HTML_FILE = "data-table-widget-current-object.html";
	private static final String DATA_TABLE_WIDGET_CURRENT_OBJECT_ID = "8b987e70-7c6c-4806-d08c-6beb4f461275";
	private static final String WIDGET_TITLE = "Fourth Widget";
	private static final String DATA_TABLE_WIDGET_CURRENT_OBJECT_HEADING_TEXT = "Sixth Widget";
	private static final String INSTANCE_HEADER = "<span><img src=\"some image src\" /></span><span><a class=\"instance-link has-tooltip\" href=\"#/idoc/emf:a117c235-e5f7-4417-b45c-c637db3a09e4\">(<span data-property=\"type\">Common document</span>) <span data-property=\"title\">barnat.pdf</span></a></span>";
	private static final String WARN_MESSAGE_NO_OBJECT_COULD_BE_FOUND = "No object could be found with the selected search criteria.";
	private static final String WARN_MESSAGE_NO_SELECTION = "No Selection.";
	private static final String TEST_FILE_AUTO_MODE_NO_OBJECTS_FOUND = "datatable-widget-automatically-mode-no-objects-found.json";
	private static final String TEST_FILE_MANUAL_MODE_NO_SELECTED_OBJECTS = "datatable-widget-manually-mode-no-objects-found.json";

	private static final String TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN = "dtw-with-columns-order-without-entity-column.json";
	private static final String TEST_DTW_WITH_COLUMN_ORDER_WITH_ENTITY_COLUMN = "dtw-with-columns-order-with-entity-column.json";

	private static final String TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW = "dtw-with-columns-order-without-entity-column-without-header-row.json";
	private static final String TEST_DTW_WITH_COLUMN_ORDER_WITH_ENTITY_COLUMN_WITHOUT_HEADER_ROW = "dtw-with-columns-order-with-entity-column-without-header-row.json";

	private static final String DTW_WITH_COLUMNS_ORDER_WITHOUT_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON = "dtw-with-columns-order-without-entity-column-with-table-title.json";
	private static final String DTW_WITH_COLUMNS_ORDER_WITH_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON = "dtw-with-columns-order-with-entity-column-with-table-title.json";

	private static final String TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW_WITH_TITLE = "dtw-with-columns-order-without-entity-column-without-header-row-with-title.json";
	private static final String TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITH_HEADER_ROW_WITH_TITLE = "dtw-with-columns-order-with-entity-column-without-header-row-with-title.json";

	private static final String TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN = "dtw-without-columns-order-without-entity-column.json";
	private static final String TEST_DTW_WITHOUT_COLUMN_ORDER_WITH_ENTITY_COLUMN = "dtw-without-columns-order-with-entity-column.json";

	private static final String TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW = "dtw-without-columns-order-without-entity-column-without-header-row.json";
	private static final String TEST_DTW_WITHOUT_COLUMN_ORDER_WITH_ENTITY_COLUMN_WITHOUT_HEADER_ROW = "dtw-without-columns-order-with-entity-column-without-header-row.json";

	private static final String DTW_WITHOUT_COLUMNS_ORDER_WITHOUT_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON = "dtw-without-columns-order-without-entity-column-with-table-title.json";
	private static final String DTW_WITHOUT_COLUMNS_ORDER_WITH_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON = "dtw-without-columns-order-with-entity-column-with-table-title.json";

	private static final String TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW_WITH_TITLE = "dtw-without-columns-order-without-entity-column-without-header-row-with-title.json";
	private static final String TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITH_HEADER_ROW_WITH_TITLE = "dtw-without-columns-order-with-entity-column-without-header-row-with-title.json";

	@InjectMocks
	private DataTableWidget dataTableWidget;

	@Mock
	private SearchService searchService;

	@Mock
	private InstanceTypeResolver instanceResolver;

	@Mock
	private LinkProviderService linkProviderService;

	@Mock
	private CodelistService codelistService;

	@Mock
	private InstanceLoadDecorator instanceDecorator;

	@Mock
	private SystemConfiguration systemConfiguration;

	@Spy
	private JsonToConditionConverter convertor = new JsonToConditionConverter();

	@Mock
	private DictionaryService dictionaryService;

	@Mock
	private LabelProvider labelProvider;

	@Mock
	private TypeConverter typeConverter;

	@Mock
	private JsonToDateRangeConverter jsonToDateRangeConverter;

	@Mock
	private ConditionsManager conditionsManager;

	/**
	 * Runs Before class init.
	 */
	@BeforeClass
	public static void beforeClass() {
		ContentNodeFactory instance = ContentNodeFactory.getInstance();
		instance.registerBuilder(new LayoutNodeBuilder());
		instance.registerBuilder(new LayoutManagerBuilder());
	}

	/**
	 * Runs before each method and setup mockito.
	 */
	@BeforeMethod
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(systemConfiguration.getUi2Url()).thenReturn(new ConfigurationPropertyMock<>(UI2_URL));
		Mockito.when(labelProvider.getLabel(IdocRenderer.KEY_LABEL_SEARCH_RESULTS)).thenReturn("Results");
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. With entity column. 3. With title. 4. With header row.
	 * 5. Inner table.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithEntityColumnWithoutHeaderRowWithTitleInnerTabelTest()
			throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITH_HEADER_ROW_WITH_TITLE);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_TD), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(),
				TABLE_TITLE + " " + TYPE + " " + ENTITY + " " + TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelOne
						+ " " + INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelTwo + " "
						+ INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 7);
		Assert.assertEquals(table.attr(JsoupUtil.ATTRIBUTE_WIDTH), "99%");

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p strong").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p strong").text(), ENTITY);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p strong").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(3) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(3) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(2) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(4) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. With entity column. 3. With title. 4. With header row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithEntityColumnWithoutHeaderRowWithTitleTest()
			throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITH_HEADER_ROW_WITH_TITLE);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(),
				TABLE_TITLE + " " + TYPE + " " + ENTITY + " " + TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelOne
						+ " " + INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelTwo + " "
						+ INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 7);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p strong").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p strong").text(), ENTITY);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p strong").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(3) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(3) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(2) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(4) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. With entity column. 3. With title. 4. Without header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithEntityColumnWithTitleTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				DTW_WITH_COLUMNS_ORDER_WITH_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(),
				TABLE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelOne + " " + INSTANCE_ONE_TITLE + " "
						+ CL210_TITLE_DESCRIPTION + " " + labelTwo + " " + INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 6);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. With entity column. 3. Without title. 4. Without header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithEntityColumnWithoutHeaderRowTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITH_COLUMN_ORDER_WITH_ENTITY_COLUMN_WITHOUT_HEADER_ROW);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(), CL210_TITLE_DESCRIPTION + " " + labelOne + " " + INSTANCE_ONE_TITLE + " "
				+ CL210_TITLE_DESCRIPTION + " " + labelTwo + " " + INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 5);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(0) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(0) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. With entity column. 3. Without title.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithEntityColumnTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(TEST_DTW_WITH_COLUMN_ORDER_WITH_ENTITY_COLUMN);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(),
				TYPE + " " + ENTITY + " " + TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelOne + " "
						+ INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelTwo + " " + INSTANCE_TWO_TITLE
						+ " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 6);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p strong").text(), ENTITY);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p strong").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. Without entity column. 3. With title. 4. With header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithoutEntityColumnWithoutHeaderRowWithTitleTest()
			throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW_WITH_TITLE);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), TABLE_TITLE + " " + TYPE + " " + TITLE + " " + CL210_TITLE_DESCRIPTION + " "
				+ INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 5);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p strong").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p strong").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(4) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. Without entity column. 3. With title. 4. Without header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithoutEntityColumnWithTitleTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				DTW_WITH_COLUMNS_ORDER_WITHOUT_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), TABLE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + INSTANCE_ONE_TITLE + " "
				+ CL210_TITLE_DESCRIPTION + " " + INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 4);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. Without entity column. 3. Without title. 4. Without
	 * header row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithoutEntityColumnWithoutHeaderRowTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), CL210_TITLE_DESCRIPTION + " " + INSTANCE_ONE_TITLE + " "
				+ CL210_TITLE_DESCRIPTION + " " + INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 3);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. With columns order. 2. Without entity column. 3. Without title.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithColumnsOrderWithoutEntityColumnTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(TEST_DTW_WITH_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), TYPE + " " + TITLE + " " + CL210_TITLE_DESCRIPTION + " " + INSTANCE_ONE_TITLE
				+ " " + CL210_TITLE_DESCRIPTION + " " + INSTANCE_TWO_TITLE + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 4);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p strong").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p strong").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. With entity column. 3. With title. 4. With header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithEntityColumnWithoutHeaderRowWithTitleTest()
			throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITH_HEADER_ROW_WITH_TITLE);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(),
				TABLE_TITLE + " " + ENTITY + " " + TITLE + " " + TYPE + " " + labelOne + " " + INSTANCE_ONE_TITLE + " "
						+ CL210_TITLE_DESCRIPTION + " " + labelTwo + " " + INSTANCE_TWO_TITLE + " "
						+ CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 7);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), ENTITY);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(3) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(3) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(4) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. With entity column. 3. With title. 4. Without header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithEntityColumnWithTitleTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				DTW_WITHOUT_COLUMNS_ORDER_WITH_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(),
				TABLE_TITLE + " " + labelOne + " " + INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + labelTwo
						+ " " + INSTANCE_TWO_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 6);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. With entity column. 3. Without title. 4. Without
	 * header row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithEntityColumnWithoutHeaderRowTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITHOUT_COLUMN_ORDER_WITH_ENTITY_COLUMN_WITHOUT_HEADER_ROW);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(), labelOne + " " + INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " "
				+ labelTwo + " " + INSTANCE_TWO_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 5);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(0) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(0) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. With entity column. 3. Without title.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithEntityColumnTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(TEST_DTW_WITHOUT_COLUMN_ORDER_WITH_ENTITY_COLUMN);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);
		Assert.assertEquals(table.text(),
				ENTITY + " " + TITLE + " " + TYPE + " " + labelOne + " " + INSTANCE_ONE_TITLE + " "
						+ CL210_TITLE_DESCRIPTION + " " + labelTwo + " " + INSTANCE_TWO_TITLE + " "
						+ CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 6);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), ENTITY);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(2) p").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcOne);
		Assert.assertEquals(
				table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefOne);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelOne);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(0) span img").attr(JsoupUtil.ATTRIBUTE_SRC),
				UI2_URL + srcTwo);
		Assert.assertEquals(
				table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(1) span a").attr(JsoupUtil.ATTRIBUTE_HREF),
				UI2_URL + hrefTwo);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) table tr:eq(0) td:eq(1) span a").text(), labelTwo);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(2)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(2) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "3");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. Without entity column. 3. With title. 4. With header
	 * row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithoutEntityColumnWithoutHeaderRowWithTitleTest()
			throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW_WITH_TITLE);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), TABLE_TITLE + " " + TITLE + " " + TYPE + " " + INSTANCE_ONE_TITLE + " "
				+ CL210_TITLE_DESCRIPTION + " " + INSTANCE_TWO_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 5);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(4) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(4) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. Without entity column. 3. With title. 4. Without
	 * header row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithoutEntityColumnWithTitleTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				DTW_WITHOUT_COLUMNS_ORDER_WITHOUT_ENTITY_COLUMN_WITH_TABLE_TITLE_JSON);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), TABLE_TITLE + " " + INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " "
				+ INSTANCE_TWO_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 4);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), TABLE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. Without entity column. 3. Without title. 4. Without
	 * header row.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithoutEntityColumnWithoutHeaderRowTest()
			throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(
				TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN_WITHOUT_HEADER_ROW);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + INSTANCE_TWO_TITLE
				+ " " + CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 3);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), RESULTS);
	}

	/**
	 * Tests method render. Scenario: 1. Without columns order. 2. Without entity column. 3. Without title.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void renderWithoutColumnsOrderWithoutEntityColumnTest() throws URISyntaxException, IOException {
		JsonObject loadedTestResource = loadTestResource(TEST_DTW_WITHOUT_COLUMN_ORDER_WITHOUT_ENTITY_COLUMN);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		initInstancesForTest();

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.text(), TITLE + " " + TYPE + " " + INSTANCE_ONE_TITLE + " " + CL210_TITLE_DESCRIPTION
				+ " " + INSTANCE_TWO_TITLE + " " + CL210_TITLE_DESCRIPTION + " " + RESULTS);
		Assert.assertEquals(table.select("tr").size(), 4);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(0) p strong").text(), TITLE);

		Assert.assertEquals(table.select("tr:eq(0) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p strong").size(), 1);
		Assert.assertEquals(table.select("tr:eq(0) td:eq(1) p strong").text(), TYPE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(0) p").text(), INSTANCE_ONE_TITLE);

		Assert.assertEquals(table.select("tr:eq(1) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(1) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(0) p").text(), INSTANCE_TWO_TITLE);

		Assert.assertEquals(table.select("tr:eq(2) td:eq(1)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(2) td:eq(1) p").text(), CL210_TITLE_DESCRIPTION);

		Assert.assertEquals(table.select("tr:eq(3) td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").size(), 1);
		Assert.assertEquals(table.select("tr:eq(3) td:eq(0) p").text(), RESULTS);
	}

	private void initInstancesForTest() {
		String typeId = "OT210027";
		DataTypeDefinition textDataTypeDefinition = Mockito.mock(DataTypeDefinition.class);
		Mockito.when(textDataTypeDefinition.getName()).thenReturn("text");
		PropertyDefinitionMock propertyDefinitionTitle = new PropertyDefinitionMock();
		propertyDefinitionTitle.setLabelId(TITLE);
		propertyDefinitionTitle.setIdentifier(TITLE.toLowerCase());
		propertyDefinitionTitle.setDataType(textDataTypeDefinition);
		PropertyDefinitionMock propertyDefinitionType = new PropertyDefinitionMock();
		propertyDefinitionType.setLabelId(TYPE);
		propertyDefinitionType.setIdentifier(TYPE.toLowerCase());
		propertyDefinitionType.setCodelist(210);
		propertyDefinitionType.setDataType(textDataTypeDefinition);
		Instance instanceOne = Mockito.mock(Instance.class);
		Mockito.when(instanceOne.getString(DefaultProperties.HEADER_COMPACT)).thenReturn("<span><img src=\"" + srcOne
				+ "\" /></span><span><a href=\"" + hrefOne + "\"><span>" + labelOne + "</span></a></span>");

		Mockito.when(instanceOne.getId()).thenReturn(INSTANCE_ONE_ID);
		DefinitionModel definitionModelInstanceOne = Mockito.mock(DefinitionModel.class);
		Mockito.when(dictionaryService.getInstanceDefinition(instanceOne)).thenReturn(definitionModelInstanceOne);
		Mockito.when(definitionModelInstanceOne.getField(TITLE.toLowerCase())).thenReturn(
				Optional.of(propertyDefinitionTitle));
		Mockito.when(definitionModelInstanceOne.getField(TYPE.toLowerCase())).thenReturn(
				Optional.of(propertyDefinitionType));
		Mockito.when(dictionaryService.getProperty(TITLE.toLowerCase(), instanceOne)).thenReturn(
				propertyDefinitionTitle);
		Mockito.when(dictionaryService.getProperty(TYPE.toLowerCase(), instanceOne)).thenReturn(propertyDefinitionType);
		Mockito.when(instanceOne.get(TITLE.toLowerCase())).thenReturn(INSTANCE_ONE_TITLE);
		Mockito.when(instanceOne.get(TYPE.toLowerCase())).thenReturn(typeId);
		Mockito.when(codelistService.getDescription(CL_210, typeId)).thenReturn(CL210_TITLE_DESCRIPTION);
		Instance instanceTwo = Mockito.mock(Instance.class);
		Mockito.when(instanceTwo.getString(DefaultProperties.HEADER_COMPACT)).thenReturn("<span><img src=\"" + srcTwo
				+ "\" /></span><span><a href=\"" + hrefTwo + "\"><span>" + labelTwo + "</span></a></span>");

		Mockito.when(instanceTwo.getId()).thenReturn(INSTANCE_TWO_ID);
		DefinitionModel definitionModelInstanceTwo = Mockito.mock(DefinitionModel.class);
		Mockito.when(dictionaryService.getInstanceDefinition(instanceTwo)).thenReturn(definitionModelInstanceTwo);
		Mockito.when(definitionModelInstanceTwo.getField(TITLE.toLowerCase())).thenReturn(
				Optional.of(propertyDefinitionTitle));
		Mockito.when(definitionModelInstanceTwo.getField(TYPE.toLowerCase())).thenReturn(
				Optional.of(propertyDefinitionType));
		Mockito.when(dictionaryService.getProperty(TITLE.toLowerCase(), instanceTwo)).thenReturn(
				propertyDefinitionTitle);
		Mockito.when(dictionaryService.getProperty(TYPE.toLowerCase(), instanceTwo)).thenReturn(propertyDefinitionType);
		Mockito.when(instanceTwo.get(TITLE.toLowerCase())).thenReturn(INSTANCE_TWO_TITLE);
		Mockito.when(instanceTwo.get(TYPE.toLowerCase())).thenReturn(typeId);
		Mockito.when(instanceResolver.resolveInstances(Arrays.asList(INSTANCE_ONE_ID, INSTANCE_TWO_ID))).thenReturn(
				Arrays.asList(instanceOne, instanceTwo));
	}

	/**
	 * Test accept method of widget.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void testAccept() {
		WidgetNode widget = Mockito.mock(WidgetNode.class);
		Mockito.when(widget.isWidget()).thenReturn(true);
		Mockito.when(widget.getName()).thenReturn(DataTableWidget.DATA_TABLE_WIDGET_NAME);
		Assert.assertTrue(dataTableWidget.accept(widget));

		Mockito.when(widget.isWidget()).thenReturn(false);
		Assert.assertFalse(dataTableWidget.accept(widget));

		Mockito.when(widget.isWidget()).thenReturn(true);
		Mockito.when(widget.getName()).thenReturn("");
		Assert.assertFalse(dataTableWidget.accept(widget));
	}

	/**
	 * Load test resource.
	 *
	 * @param resource
	 *            the resource
	 * @return the com.google.gson. json object
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private com.google.gson.JsonObject loadTestResource(String resource) throws URISyntaxException, IOException {
		URL testJsonURL = getClass().getClassLoader().getResource(resource);
		File jsonConfiguration = new File(testJsonURL.toURI());
		try (FileReader fileReader = new FileReader(jsonConfiguration)) {
			return new JsonParser().parse(fileReader).getAsJsonObject();
		}
	}

	/**
	 * No objects found test.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void noObjectsFoundTest() throws URISyntaxException, IOException {
		com.google.gson.JsonObject loadedTestResource = loadTestResource(TEST_FILE_AUTO_MODE_NO_OBJECTS_FOUND);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		SearchArguments<Instance> searchArgs = Mockito.mock(SearchArguments.class);
		when(searchArgs.getResult()).thenReturn(Collections.emptyList());
		when(searchArgs.getStringQuery()).thenReturn("");
		Sorter sorter = Mockito.mock(Sorter.class);
		List<Sorter> sorters = new ArrayList<>(1);
		sorters.add(sorter);
		when(searchArgs.getSorters()).thenReturn(sorters);
		when(searchService.parseRequest(Matchers.any(SearchRequest.class))).thenReturn(searchArgs);

		when(instanceResolver.resolveInstances(Matchers.anyCollection())).thenReturn(Collections.emptyList());

		Mockito.when(labelProvider.getLabel(IdocRenderer.KEY_LABEL_SELECT_OBJECT_RESULTS_NONE)).thenReturn(
				WARN_MESSAGE_NO_OBJECT_COULD_BE_FOUND);

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.tagName(), JsoupUtil.TAG_TABLE);
		Assert.assertEquals(table.select("tr:eq(0) > td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) > td:eq(0) p").text(), WIDGET_TITLE);
		Assert.assertEquals(table.select("tr:eq(1) > td:eq(0)").text(), WARN_MESSAGE_NO_OBJECT_COULD_BE_FOUND);
	}

	/**
	 * No selected objects test.
	 *
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void noSelectedObjectsTest() throws URISyntaxException, IOException {
		com.google.gson.JsonObject loadedTestResource = loadTestResource(TEST_FILE_MANUAL_MODE_NO_SELECTED_OBJECTS);
		WidgetNode widget = Mockito.mock(WidgetNode.class);

		Element parentOfNode = new Element(Tag.valueOf(JsoupUtil.TAG_SPAN), "");
		Element element = parentOfNode.appendElement("div");
		Mockito.when(widget.getElement()).thenReturn(element);

		when(widget.getConfiguration()).thenReturn(new WidgetConfiguration(widget, loadedTestResource));

		when(instanceResolver.resolveInstances(Matchers.anyCollection())).thenReturn(Collections.emptyList());

		Mockito.when(labelProvider.getLabel(IdocRenderer.KEY_LABEL_SELECT_OBJECT_NONE)).thenReturn(
				WARN_MESSAGE_NO_SELECTION);

		Element table = dataTableWidget.render(INSTANCE_ID, widget);

		Assert.assertEquals(table.tagName(), JsoupUtil.TAG_TABLE);
		Assert.assertEquals(table.select("tr:eq(0) > td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "1");
		Assert.assertEquals(table.select("tr:eq(0) > td:eq(0) p").text(), WIDGET_TITLE);
		Assert.assertEquals(table.select("tr:eq(1) > td:eq(0)").text(), WARN_MESSAGE_NO_SELECTION);
	}

	/**
	 * Test render method of widget.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void testRender() throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(DATA_TABLE_WIDGET_HTML_FILE)) {
			Idoc idoc = Idoc.parse(is);
			Optional<Widget> selectedWidget = idoc.selectWidget(DATA_TABLE_WIDGET_ID);
			Instance instance = Mockito.mock(Instance.class);
			when(instance.getId()).thenReturn(INSTANCE_ID);
			when(instance.get(TITLE.toLowerCase())).thenReturn(TEST_TITLE);
			when(instance.getString(DefaultProperties.HEADER_COMPACT)).thenReturn(INSTANCE_HEADER);
			DefinitionModel instanceDefinitionModel = Mockito.mock(DefinitionModel.class);
			PropertyDefinitionMock propertyDefinition = new PropertyDefinitionMock();
			propertyDefinition.setLabelId(TITLE);
			propertyDefinition.setIdentifier(TITLE.toLowerCase());
			DataTypeDefinition dataTypeDefinition = mock(DataTypeDefinition.class);
			propertyDefinition.setDataType(dataTypeDefinition);
			when(dataTypeDefinition.getName()).thenReturn(DataTypeDefinition.TEXT);
			when(dictionaryService.getProperty(TITLE.toLowerCase(), instance)).thenReturn(propertyDefinition);
			when(dictionaryService.getInstanceDefinition(instance)).thenReturn(instanceDefinitionModel);
			when(dictionaryService.find(Matchers.anyString())).thenReturn(instanceDefinitionModel);
			when(instanceDefinitionModel.getField(Matchers.anyString())).thenReturn(Optional.of(propertyDefinition));
			when(instanceResolver.resolveInstances(Matchers.anyCollection())).thenReturn(Arrays.asList(instance));
			Element table = dataTableWidget.render(INSTANCE_ID, selectedWidget.get());
			Assert.assertEquals(table.tagName(), JsoupUtil.TAG_TABLE);
			Assert.assertEquals(table.select("tr:eq(0) > td:eq(0)").attr(JsoupUtil.ATTRIBUTE_COLSPAN), "2");
			Assert.assertEquals(table.select("tr:eq(0) > td:eq(0) p").text(), WIDGET_TITLE);
			Assert.assertEquals(table.select("tr:eq(1) > td:eq(0) p").text(), IdocRenderer.ENTITY_LABEL);
			Assert.assertEquals(table.select("tr:eq(1) > td:eq(1) p").text(), TITLE);
			Assert.assertEquals(
					table.select("tr:eq(2) > td:eq(0) table tr:eq(0) > td:eq(1) a").attr(JsoupUtil.ATTRIBUTE_HREF),
					UI2_URL + "#/idoc/emf:a117c235-e5f7-4417-b45c-c637db3a09e4");
			Assert.assertEquals(table.select("tr:eq(2) > td:eq(0) table tr:eq(0) > td:eq(1) a").text(),
					HYPERLINK_LABEL);
			Assert.assertEquals(table.select("tr:eq(2) > td:eq(1) p").text(), TEST_TITLE);
		}
	}

	@Test
	public void testRenderAutomaticSearchWithCurrentObject() throws IOException {
		try (InputStream is = getClass()
				.getClassLoader()
					.getResourceAsStream(DATA_TABLE_WIDGET_CURRENT_OBJECT_HTML_FILE)) {
			Idoc idoc = Idoc.parse(is);
			Optional<Widget> selectedWidget = idoc.selectWidget(DATA_TABLE_WIDGET_CURRENT_OBJECT_ID);
			Instance instance = Mockito.mock(Instance.class);
			SearchArguments<Instance> searchArguments = Mockito.mock(SearchArguments.class);
			when(searchArguments.getStringQuery()).thenReturn("");
			Sorter sorter = Mockito.mock(Sorter.class);
			List<Sorter> sorters = new ArrayList<>(1);
			sorters.add(sorter);
			when(searchArguments.getSorters()).thenReturn(sorters);
			when(searchService.parseRequest(Matchers.any(SearchRequest.class))).thenReturn(searchArguments);
			when(searchArguments.getResult()).thenReturn(Arrays.asList(instance));
			when(instance.getId()).thenReturn(INSTANCE_ID);
			when(instance.get(TITLE.toLowerCase())).thenReturn(TEST_TITLE);
			when(instance.getString(DefaultProperties.HEADER_COMPACT)).thenReturn(INSTANCE_HEADER);
			DefinitionModel instanceDefinitionModel = Mockito.mock(DefinitionModel.class);
			PropertyDefinitionMock propertyDefinition = new PropertyDefinitionMock();
			propertyDefinition.setLabelId(TITLE);
			propertyDefinition.setIdentifier(TITLE.toLowerCase());
			DataTypeDefinition dataTypeDefinition = mock(DataTypeDefinition.class);
			propertyDefinition.setDataType(dataTypeDefinition);
			when(dataTypeDefinition.getName()).thenReturn(DataTypeDefinition.TEXT);
			when(dictionaryService.getProperty(TITLE.toLowerCase(), instance)).thenReturn(propertyDefinition);
			when(dictionaryService.getInstanceDefinition(instance)).thenReturn(instanceDefinitionModel);
			when(dictionaryService.find(Matchers.anyString())).thenReturn(instanceDefinitionModel);
			when(instanceDefinitionModel.getField(Matchers.anyString())).thenReturn(Optional.of(propertyDefinition));
			when(instanceResolver.resolveInstances(Matchers.anyCollection())).thenReturn(Arrays.asList(instance));
			Element table = dataTableWidget.render(INSTANCE_ID, selectedWidget.get());
			Assert.assertEquals(table.tagName(), JsoupUtil.TAG_TABLE);
			Assert.assertEquals(table.select("tr:eq(0) > td:eq(0)").attr("colspan"), "2");
			Assert.assertEquals(table.select("tr:eq(0) > td:eq(0) p").text(),
					DATA_TABLE_WIDGET_CURRENT_OBJECT_HEADING_TEXT);
			Assert.assertEquals(table.select("tr:eq(1) > td:eq(0)").text(), IdocRenderer.ENTITY_LABEL);
			Assert.assertEquals(table.select("tr:eq(1) > td:eq(1)").text(), TITLE);
			Assert.assertEquals(
					table.select("tr:eq(2) > td:eq(0) table tr:eq(0) > td:eq(1) a").attr(JsoupUtil.ATTRIBUTE_HREF),
					UI2_URL + "#/idoc/emf:a117c235-e5f7-4417-b45c-c637db3a09e4");
			Assert.assertEquals(table.select("tr:eq(2) > td:eq(0)").text(), HYPERLINK_LABEL);
			Assert.assertEquals(table.select("tr:eq(2) > td:eq(1)").text(), TEST_TITLE);
		}
	}
}