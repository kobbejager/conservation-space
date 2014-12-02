package com.sirma.itt.emf.evaluation;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The Class AndExpressionEvaluatorTest.
 * 
 * @author BBonev
 */
@Test
public class AndExpressionEvaluatorTest extends BaseEvaluatorTest {

	/**
	 * Test evaluation.
	 */
	public void testEvaluation() {
		AndExpressionEvaluator evaluator = new AndExpressionEvaluator();

		Assert.assertEquals(evaluator.evaluate("${and(true)}"), Boolean.TRUE);

		Assert.assertEquals(evaluator.evaluate("${and(true and false)}"), Boolean.FALSE);

		Assert.assertEquals(evaluator.evaluate("${and(true and true)}"), Boolean.TRUE);

		Assert.assertEquals(evaluator.evaluate("${and(true TRUE)}"), Boolean.TRUE);

		Assert.assertEquals(evaluator.evaluate("${and(true AND)}"), Boolean.TRUE);

		Assert.assertEquals(evaluator.evaluate("${and(true AND not false)}"), Boolean.TRUE);
	}
}
