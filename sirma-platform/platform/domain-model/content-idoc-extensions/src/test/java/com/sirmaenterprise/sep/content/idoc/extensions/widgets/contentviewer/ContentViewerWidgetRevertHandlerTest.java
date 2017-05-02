package com.sirmaenterprise.sep.content.idoc.extensions.widgets.contentviewer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.sirmaenterprise.sep.content.idoc.extensions.widgets.utils.WidgetMock;
import com.sirmaenterprise.sep.content.idoc.nodes.widgets.content.ContentViewerWidget;

/**
 * Test for {@link ContentViewerWidgetRevertHandler}.
 *
 * @author A. Kunchev
 */
public class ContentViewerWidgetRevertHandlerTest {

	private ContentViewerWidgetRevertHandler handler;

	@Before
	public void setup() {
		handler = new ContentViewerWidgetRevertHandler();
	}

	@Test
	public void accept_incorrectWidget_false() {
		boolean result = handler.accept(new WidgetMock());
		assertFalse(result);
	}

	@Test
	public void accept_correctWidget_true() {
		boolean result = handler.accept(mock(ContentViewerWidget.class));
		assertTrue(result);
	}

}
