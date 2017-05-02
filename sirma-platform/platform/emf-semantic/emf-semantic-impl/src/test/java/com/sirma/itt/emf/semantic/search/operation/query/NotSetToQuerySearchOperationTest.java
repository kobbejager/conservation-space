package com.sirma.itt.emf.semantic.search.operation.query;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sirma.itt.emf.semantic.SemanticSearchConfigurations;
import com.sirma.itt.emf.semantic.search.SearchOperationUtils;
import com.sirma.itt.emf.semantic.search.SearchQueryBuilder;
import com.sirma.itt.seip.domain.search.tree.Rule;
import com.sirma.itt.seip.search.converters.JsonToConditionConverter;

/**
 * Test the not set to query search operation building.
 *
 * @author nvelkov
 */
public class NotSetToQuerySearchOperationTest {

	@Mock
	private SearchQueryBuilder searchQueryBuilder;

	@Mock
	private JsonToConditionConverter converter;

	@Mock
	private SemanticSearchConfigurations configurations;

	@InjectMocks
	private QuerySearchOperation operation = new NotSetToQuerySearchOperation();

	@Before
	public void initialize() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Test the is applicable method. The rule should be applicable if the operation is "not_set_to_query".
	 */
	@Test
	public void testIsApplicable() {
		QuerySearchOperationTestHelper.mockConverter(converter);

		Rule rule = SearchOperationUtils.createRule("field", "object", "not_set_to_query", "{\"id\":\"asd\"}");
		Assert.assertTrue("The operation isn't applicable but it should be", operation.isApplicable(rule));

		rule = SearchOperationUtils.createRule("field", "object", "not_set_to", "{\"id\":\"asd\"}");
		Assert.assertFalse("The operation is applicable but it shouldn't be", operation.isApplicable(rule));
	}

	/**
	 * Test the build Operation method. The returned result should contain the generated query with a generated embedded
	 * query with the instance variable replaced.
	 */
	@Test
	public void testBuildOperation() {
		QuerySearchOperationTestHelper.mockConverter(converter);
		QuerySearchOperationTestHelper.mockConfigurations(configurations, "sectionInstance, caseInstance");
		QuerySearchOperationTestHelper.mockSearchQueryBuilder(searchQueryBuilder, "{?instance emf:Relation emf:Admin}");
		Pattern pattern = Pattern
				.compile("\\{\\{SELECT (?!\\\\?instance).* WHERE\\{ .* emf:isDeleted \"false\"\\^\\^xsd:boolean \\.  "
						+ QuerySearchOperationTestHelper.PATTERN_INSTANCE_TYPES + System.lineSeparator()
						+ "\\{(?!\\?instance).* emf:Relation emf:Admin\\}\\}\\}\\{ FILTER \\( NOT EXISTS  \\{  \\?instance (?!\\?instance).* (?!\\?instance).* \\. \\} \\) \\}\\}");
		Assert.assertTrue("The returned result didn't match the expression ", QuerySearchOperationTestHelper
				.operationMatches(operation, pattern, "{?instance emf:Relation emf:Admin}"));
	}
}
