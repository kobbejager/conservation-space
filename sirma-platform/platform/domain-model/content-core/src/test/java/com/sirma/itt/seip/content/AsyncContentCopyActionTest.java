package com.sirma.itt.seip.content;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sirma.itt.seip.content.ContentPersistProvider.ContentPersister;
import com.sirma.itt.seip.content.ContentPersistProvider.PreviousVersion;
import com.sirma.itt.seip.exception.EmfRuntimeException;
import com.sirma.itt.seip.tasks.SchedulerContext;

/**
 * Test for {@link AsyncContentCopyAction}
 *
 * @author BBonev
 */
public class AsyncContentCopyActionTest {

	private static final String TARGET_CONTENT_ID = "targetContentId";
	private static final String SOURCE_CONTENT_ID = "sourceContentId";
	private static final String STORE_NAME = "remoteSystemName";

	@InjectMocks
	private AsyncContentCopyAction action;

	@Mock
	private ContentStoreProvider storeProvider;
	@Mock
	private InstanceContentService contentService;
	@Mock
	private ContentEntityDao entityDao;
	@Mock
	private ContentPersistProvider contentPersistProvider;
	@Mock
	private ContentPersister contentPersister;

	@Mock
	private ContentStore store;

	@Before
	public void beforeMethod() {
		MockitoAnnotations.initMocks(this);
		when(contentPersistProvider.getPersister(any(Content.class))).thenReturn(contentPersister);

	}

	@Test
	public void shouldCopyContentIfExists() throws Exception {
		mockValidContentInfo();
		mockValidStore();
		mockValidEntity();

		action.execute(createContext());

		verify(contentPersister).uploadContentAndPersist(any(), any(), any(), eq(PreviousVersion.NO_PREVIOUS_VERSION),
				eq(null), eq(store), eq(true), eq(false));
	}

	@Test(expected = EmfRuntimeException.class)
	public void shouldFailIfSourceContentIsNotFound() throws Exception {
		when(contentService.getContent(SOURCE_CONTENT_ID, null)).thenReturn(mock(ContentInfo.class));

		action.execute(createContext());
	}

	@Test(expected = EmfRuntimeException.class)
	public void shouldFailIfSourceStoreIsNotFound() throws Exception {
		mockValidContentInfo();

		action.execute(createContext());
	}

	@Test(expected = EmfRuntimeException.class)
	public void shouldFailIfTargetEntityIsNotFound() throws Exception {
		mockValidContentInfo();
		mockValidStore();

		action.execute(createContext());
	}

	private static SchedulerContext createContext() {
		SchedulerContext context = new SchedulerContext();
		context.put(AsyncContentCopyAction.SOURCE_CONTENT_ID_KEY, SOURCE_CONTENT_ID);
		context.put(AsyncContentCopyAction.TARGET_CONTENT_ID_KEY, TARGET_CONTENT_ID);
		return context;
	}

	private void mockValidContentInfo() {
		ContentInfo info = mock(ContentInfo.class);
		when(info.exists()).thenReturn(Boolean.TRUE);
		when(info.getRemoteSourceName()).thenReturn(STORE_NAME);
		when(contentService.getContent(SOURCE_CONTENT_ID, null)).thenReturn(info);
	}

	private void mockValidStore() {
		when(store.getName()).thenReturn(STORE_NAME);
		when(storeProvider.getStore(STORE_NAME)).thenReturn(store);
	}

	private void mockValidEntity() {
		ContentEntity entity = new ContentEntity();
		when(entityDao.getEntity(TARGET_CONTENT_ID, "any")).thenReturn(entity);
	}
}
