package com.sirma.sep.content.idoc.extensions.widgets.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.sirma.sep.content.idoc.extensions.widgets.image.ImageWidgetSearchHandler;
import com.sirma.sep.content.idoc.extensions.widgets.utils.WidgetMock;
import com.sirma.sep.content.idoc.nodes.widgets.image.ImageWidget;

/**
 * Test for {@link ImageWidgetSearchHandler}.
 *
 * @author A. Kunchev
 */
public class ImageWidgetSearchHandlerTest {

	private ImageWidgetSearchHandler handler;

	@Before
	public void setup() {
		handler = new ImageWidgetSearchHandler();
	}

	@Test
	public void accept_incorrectType_false() {
		boolean result = handler.accept(new WidgetMock());
		assertFalse(result);
	}

	@Test
	public void accept_correctType_true() {
		boolean result = handler.accept(mock(ImageWidget.class));
		assertTrue(result);
	}

}
