package com.sirmaenterprise.sep.bpm.camunda.bpmn.execution.listeners;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.camunda.bpm.engine.impl.el.Expression;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.InstanceReference;
import com.sirma.itt.seip.instance.DomainInstanceService;
import com.sirma.itt.seip.instance.HeadersService;
import com.sirma.itt.seip.instance.InstanceSaveContext;
import com.sirma.itt.seip.instance.InstanceTypeResolver;
import com.sirma.itt.seip.instance.lock.LockInfo;
import com.sirma.itt.seip.instance.lock.exception.LockException;
import com.sirma.itt.seip.instance.relation.LinkReference;
import com.sirma.itt.seip.instance.relation.LinkService;
import com.sirma.itt.seip.testutil.CustomMatcher;
import com.sirma.itt.seip.util.ReflectionUtils;
import com.sirmaenterprise.sep.bpm.camunda.MockProvider;
import com.sirmaenterprise.sep.bpm.camunda.exception.CamundaIntegrationRuntimeException;

/**
 * Tests {@link ChangeObjectState}
 *
 * @author bbanchev
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangeObjectStateTest {

	@Mock
	private DomainInstanceService domainInstanceService;

	@Mock
	private InstanceTypeResolver instanceTypeResolver;

	@Mock
	private HeadersService headersService;

	@Mock
	private LinkService linkService;

	@Mock
	private FixedValue relation;

	@Mock
	private Expression source;

	@Mock
	private Expression status;

	@InjectMocks
	private ChangeObjectState changeObjectState;

	@Test
	public void testValidateParametersWithValidParameters() throws Exception {
		changeObjectState.validateParameters();
	}

	@Test(expected = Exception.class)
	public void testValidateParametersWithNullParameters() throws Exception {
		ReflectionUtils.setFieldValue(changeObjectState, "source", null);
		changeObjectState.validateParameters();
	}

	@Test
	public void testExecute() throws Exception {
		ActivityExecution delegateExecution = MockProvider.mockDelegateExecution(MockProvider.DEFAULT_ENGINE,
				ActivityExecution.class);
		when(delegateExecution.getProcessInstance()).thenReturn(delegateExecution);
		when(delegateExecution.getBusinessKey()).thenReturn("emf:uuid");
		when(source.getValue(eq(delegateExecution))).thenReturn(delegateExecution);
		when(status.getValue(eq(delegateExecution))).thenReturn("TEST");
		when(relation.getValue(eq(delegateExecution))).thenReturn("emf:relation");

		Instance instance = mock(Instance.class);

		InstanceReference instanceRef = mock(InstanceReference.class);
		when(instanceRef.toInstance()).thenReturn(instance);
		when(instance.toReference()).thenReturn(instanceRef);
		when(instanceRef.getId()).thenReturn("emf:uuid");

		InstanceReference relatedRef = mock(InstanceReference.class);
		when(relatedRef.getId()).thenReturn("emf:relatedId");
		Instance related = mock(Instance.class);
		when(relatedRef.toInstance()).thenReturn(related);
		when(instanceTypeResolver.resolveReference(eq("emf:relatedId"))).thenReturn(Optional.of(relatedRef));
		LinkReference linkRef = mock(LinkReference.class);
		when(linkRef.getFrom()).thenReturn(relatedRef);
		when(linkRef.getTo()).thenReturn(instanceRef);

		when(linkService.getLinks(eq(instanceRef), eq("emf:relation"))).thenReturn(Collections.singletonList(linkRef));

		Collection<Instance> instances = Collections.singletonList(related);
		when(instanceTypeResolver.resolveReference(eq("emf:uuid"))).thenReturn(Optional.of(instanceRef));
		when(instanceTypeResolver.resolveInstances(eq(Collections.singletonList("emf:relatedId"))))
				.thenReturn(instances);

		changeObjectState.execute(delegateExecution, changeObjectState);

		verify(domainInstanceService).save(argThat(CustomMatcher.of((InstanceSaveContext context) -> {
			assertEquals(related, context.getInstance());
			assertEquals("dynamicBPMNStatusChange-emf:relation", context.getOperation().getUserOperationId());
		})));
	}

	@Test(expected = CamundaIntegrationRuntimeException.class)
	public void should_throw_lock_exception_on_save() throws Exception {
		ActivityExecution delegateExecution = MockProvider.mockDelegateExecution(MockProvider.DEFAULT_ENGINE,
				ActivityExecution.class);
		when(delegateExecution.getProcessInstance()).thenReturn(delegateExecution);
		when(delegateExecution.getBusinessKey()).thenReturn("emf:uuid");
		when(source.getValue(eq(delegateExecution))).thenReturn(delegateExecution);
		when(status.getValue(eq(delegateExecution))).thenReturn("TEST");
		when(relation.getValue(eq(delegateExecution))).thenReturn("emf:relation");

		Instance instance = mock(Instance.class);

		InstanceReference instanceRef = mock(InstanceReference.class);
		when(instanceRef.toInstance()).thenReturn(instance);
		when(instance.toReference()).thenReturn(instanceRef);
		when(instanceRef.getId()).thenReturn("emf:uuid");

		InstanceReference relatedRef = mock(InstanceReference.class);
		when(relatedRef.getId()).thenReturn("emf:relatedId");
		Instance related = mock(Instance.class);
		when(relatedRef.toInstance()).thenReturn(related);
		when(instanceTypeResolver.resolveReference(eq("emf:relatedId"))).thenReturn(Optional.of(relatedRef));
		LinkReference linkRef = mock(LinkReference.class);
		when(linkRef.getFrom()).thenReturn(relatedRef);
		when(linkRef.getTo()).thenReturn(instanceRef);

		when(linkService.getLinks(eq(instanceRef), eq("emf:relation"))).thenReturn(Collections.singletonList(linkRef));

		Collection<Instance> instances = Collections.singletonList(related);
		when(instanceTypeResolver.resolveReference(eq("emf:uuid"))).thenReturn(Optional.of(instanceRef));
		when(instanceTypeResolver.resolveInstances(eq(Collections.singletonList("emf:relatedId"))))
				.thenReturn(instances);
		LockInfo lockInfo = new LockInfo(relatedRef, null, null, null, (user) -> true);
		when(domainInstanceService.save(any(InstanceSaveContext.class))).thenThrow(new LockException(lockInfo, "test"));

		changeObjectState.execute(delegateExecution, changeObjectState);

	}

}
