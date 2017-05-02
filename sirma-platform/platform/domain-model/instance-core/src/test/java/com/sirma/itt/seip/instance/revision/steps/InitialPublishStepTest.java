package com.sirma.itt.seip.instance.revision.steps;

import static com.sirma.itt.seip.domain.instance.DefaultProperties.CONTENT;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sirma.itt.seip.content.Content;
import com.sirma.itt.seip.content.ContentInfo;
import com.sirma.itt.seip.content.InstanceContentService;
import com.sirma.itt.seip.domain.instance.EmfInstance;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.exception.RollbackedRuntimeException;
import com.sirma.itt.seip.instance.revision.PublishInstanceRequest;
import com.sirma.itt.seip.instance.state.Operation;
import com.sirma.itt.seip.template.TemplateService;
import com.sirma.itt.seip.template.TemplateInstance;

/**
 * Test for {@link InitialPublishStep}
 *
 * @author BBonev
 */
public class InitialPublishStepTest {
	@InjectMocks
	private InitialPublishStep step;
	@Mock
	private InstanceContentService contentService;
	@Mock
	private TemplateService templateService;

	@Before
	public void beforeMethod() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldLoadTheViewWhenExists() throws Exception {
		when(contentService.getContent(any(Instance.class), eq(Content.PRIMARY_VIEW))).then(a -> {
			ContentInfo info = mock(ContentInfo.class);
			when(info.exists()).thenReturn(Boolean.TRUE);
			when(info.getInputStream()).then(aa -> getTemplateStream());
			return info;
		});

		PublishInstanceRequest request = new PublishInstanceRequest(new EmfInstance(), new Operation(), null, null);
		PublishContext context = new PublishContext(request, new EmfInstance());

		step.execute(context);
		assertNotNull(context.getView());
	}

	@Test
	public void shouldLoadDefaultTemplateIfNoViewIsAvailable() throws Exception {
		when(contentService.getContent(any(Instance.class), eq(Content.PRIMARY_VIEW)))
				.then(a -> mock(ContentInfo.class));
		when(templateService.getPrimaryTemplate(anyString())).thenReturn(new TemplateInstance());
		when(templateService.loadContent(any(TemplateInstance.class))).then(a -> {
			TemplateInstance templateInstance = a.getArgumentAt(0, TemplateInstance.class);
			templateInstance.add(CONTENT, IOUtils.toString(getTemplateStream()));
			return templateInstance;
		});

		Instance instance = new EmfInstance();
		instance.setIdentifier("genericDocument");
		PublishInstanceRequest request = new PublishInstanceRequest(instance, new Operation(), null, null);
		PublishContext context = new PublishContext(request, new EmfInstance());

		step.execute(context);
		assertNotNull(context.getView());
	}

	@Test(expected = RollbackedRuntimeException.class)
	public void shouldFailIfFailToLoadLoadView() throws Exception {
		when(contentService.getContent(any(Instance.class), eq(Content.PRIMARY_VIEW))).then(a -> {
			ContentInfo info = mock(ContentInfo.class);
			when(info.exists()).thenReturn(Boolean.TRUE);
			InputStream inputStream = mock(InputStream.class);
			when(inputStream.read(any(byte[].class))).thenThrow(new IOException());
			when(info.getInputStream()).then(aa -> inputStream);
			return info;
		});

		Instance instance = new EmfInstance();
		instance.setIdentifier("genericDocument");
		PublishInstanceRequest request = new PublishInstanceRequest(instance, new Operation(), null, null);
		PublishContext context = new PublishContext(request, new EmfInstance());

		step.execute(context);
	}

	@Test(expected = RollbackedRuntimeException.class)
	public void shouldFailIfCantLoadDefaultTemplateAndNoViewIsAvailable() throws Exception {
		when(contentService.getContent(any(Instance.class), eq(Content.PRIMARY_VIEW)))
				.then(a -> mock(ContentInfo.class));
		when(templateService.getPrimaryTemplate(anyString())).thenReturn(new TemplateInstance());
		when(templateService.loadContent(any(TemplateInstance.class)))
				.then(a -> a.getArgumentAt(0, TemplateInstance.class));

		Instance instance = new EmfInstance();
		instance.setIdentifier("genericDocument");
		PublishInstanceRequest request = new PublishInstanceRequest(instance, new Operation(), null, null);
		PublishContext context = new PublishContext(request, new EmfInstance());

		step.execute(context);
	}

	private InputStream getTemplateStream() {
		return InitialPublishStepTest.class.getResourceAsStream("/publish-idoc.html");
	}
}
