package com.sirma.itt.seip.search;

import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sirma.itt.seip.collections.CollectionUtils;
import com.sirma.itt.seip.context.Contextual;
import com.sirma.itt.seip.context.ContextualReference;
import com.sirma.itt.seip.definition.DictionaryService;
import com.sirma.itt.seip.domain.definition.DefinitionModel;
import com.sirma.itt.seip.domain.definition.GenericDefinition;
import com.sirma.itt.seip.domain.definition.PropertyDefinition;
import com.sirma.itt.seip.search.facet.DateRangeConfigTransformer;
import com.sirma.itt.seip.testutil.mocks.PropertyDefinitionMock;
import com.sirma.itt.seip.time.DateRangeConfig;

/**
 * Tests the logic in {@link AdvancedSearchConfigurationProvider}.
 *
 * @author Mihail Radkov
 */
public class AdvancedSearchConfigurationProviderTest {

	private static final String DEFINITION_TYPE = AdvancedSearchConfigurationProviderImpl.DEFINTION_TYPE;

	@Mock
	private DictionaryService dictionaryService;

	@Mock
	private DateRangeConfigTransformer transformer;

	@Spy
	Contextual<AdvancedSearchConfiguration> configuration = new ContextualReference<>(() -> "test");

	@InjectMocks
	private AdvancedSearchConfigurationProvider facetConfigurationProvider = new AdvancedSearchConfigurationProviderImpl();

	/**
	 * Initializes Mockito mocks.
	 */
	@BeforeMethod
	public void setUp() {
		facetConfigurationProvider = new AdvancedSearchConfigurationProviderImpl();
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Tests the configuration reseting without any present definition for the advanced search configuration.
	 */
	@Test
	public void testWithoutDefinition() {
		List<DefinitionModel> definitions = getDefinitions(false);
		mockDictionaryService(definitions);

		facetConfigurationProvider.reset();

		AdvancedSearchConfiguration configuration = facetConfigurationProvider.getConfiguration();
		Assert.assertNull(configuration);
	}

	/**
	 * Tests the configuration reseting with a present definition for the advanced search configuration <b>but</b>
	 * without any fields.
	 */
	@Test
	public void testWithDefinitionWithoutFields() {
		List<DefinitionModel> definitions = getDefinitions(true);
		mockDictionaryService(definitions);

		facetConfigurationProvider.reset();

		AdvancedSearchConfiguration configuration = facetConfigurationProvider.getConfiguration();
		assertNotNull(configuration);
		Assert.assertTrue(CollectionUtils.isEmpty(configuration.getDateRanges()));
	}

	/**
	 * Tests the configuration reseting with a present definition for the advanced search configuration and with present
	 * fields.
	 */
	@Test
	public void testWithDefinitionWithFields() {
		GenericDefinition genericDefinition = mockGenericDefinition(DEFINITION_TYPE);
		PropertyDefinition propertyDefinition = new PropertyDefinitionMock();
		Mockito.when(genericDefinition.getFields()).thenReturn(Arrays.asList(propertyDefinition));

		mockDictionaryService(Arrays.asList(genericDefinition));
		mockTransformer();

		facetConfigurationProvider.reset();

		AdvancedSearchConfiguration configuration = facetConfigurationProvider.getConfiguration();
		Assert.assertNotNull(configuration);

		List<DateRangeConfig> dateRanges = configuration.getDateRanges();
		Assert.assertFalse(dateRanges.isEmpty());
	}

	/**
	 * Creates a list of {@link GenericDefinition} for testing purposes based on the provided boolean parameters.
	 *
	 * @param definitionExists
	 *            - if true adds the advanced search configuration definition to the list
	 * @return the list with test data
	 */
	private List<DefinitionModel> getDefinitions(boolean definitionExists) {
		List<DefinitionModel> definitions = new ArrayList<>();
		definitions.add(mockGenericDefinition("another_one"));
		if (definitionExists) {
			String name = DEFINITION_TYPE;
			definitions.add(mockGenericDefinition(name));
		}
		return definitions;
	}

	/**
	 * Mocks the injected {@link DictionaryService} to return the provided definitions.
	 *
	 * @param definitions
	 *            - the provided definitions
	 */
	private void mockDictionaryService(List<DefinitionModel> definitions) {
		Mockito.when(dictionaryService.getAllDefinitions(GenericDefinition.class)).thenReturn(definitions);
	}

	/**
	 * Mocks the injected {@link DateRangeConfigTransformer} to return dummy {@link DateRangeConfig}
	 */
	private void mockTransformer() {
		List<DateRangeConfig> ranges = new ArrayList<>();
		ranges.add(new DateRangeConfig());
		Mockito.when(transformer.extractDateRanges(Matchers.anyListOf(PropertyDefinition.class))).thenReturn(ranges);
	}

	/**
	 * Mock a generic definition that will return the specified definition type.
	 *
	 * @param definitionType
	 *            the definition type
	 * @return the mocked generic definition
	 */
	private GenericDefinition mockGenericDefinition(String definitionType) {
		GenericDefinition definition = Mockito.mock(GenericDefinition.class);
		Mockito.when(definition.getType()).thenReturn(definitionType);
		Mockito.when(definition.getFields()).thenReturn(CollectionUtils.<PropertyDefinition> emptyList());
		return definition;
	}
}
