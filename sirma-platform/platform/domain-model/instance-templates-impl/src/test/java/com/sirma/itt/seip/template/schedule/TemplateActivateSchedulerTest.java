package com.sirma.itt.seip.template.schedule;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sirma.itt.seip.exception.EmfRuntimeException;
import com.sirma.itt.seip.tasks.SchedulerContext;
import com.sirma.itt.seip.template.TemplateService;
import com.sirma.itt.seip.testutil.fakes.TransactionSupportFake;

/**
 * Tests for {@link TemplateActivateScheduler}
 *
 * @author hlungov
 */
public class TemplateActivateSchedulerTest {

	@Mock
	private TemplateService templateService;

	@Spy
	private TransactionSupportFake transactionSupport;

	@InjectMocks
	private TemplateActivateScheduler templateActivateScheduler;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void test_execute() throws Exception {
		SchedulerContext schedulerContext = TemplateActivateScheduler
				.createExecutorContext(Collections.singletonList("testId"));
		templateActivateScheduler.execute(schedulerContext);
		verify(templateService).activate(eq("testId"));
	}

	@Test(expected = EmfRuntimeException.class)
	public void test_createExecutorContext_null() {
		TemplateActivateScheduler.createExecutorContext(null);
	}

	@Test
	public void test_createExecutorContext() {
		List<String> idsForActivation = Collections.singletonList("testId");
		SchedulerContext executorContext = TemplateActivateScheduler
				.createExecutorContext(idsForActivation);
		Assert.assertTrue(!executorContext.isEmpty());
		Assert.assertEquals(idsForActivation,
				executorContext.getIfSameType(TemplateActivateScheduler.CORRESPONDING_INSTANCE_IDS, List.class));
	}

}
