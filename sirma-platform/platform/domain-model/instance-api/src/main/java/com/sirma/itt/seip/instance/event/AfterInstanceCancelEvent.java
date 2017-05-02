package com.sirma.itt.seip.instance.event;

import com.sirma.itt.seip.annotation.Documentation;
import com.sirma.itt.seip.domain.event.AbstractInstanceTwoPhaseEvent;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.event.TwoPhaseEvent;

/**
 * Event fired after a instance has been canceled. The event will carry the instance that has been canceled.<br>
 * <b>NOTE</b>: not all operations that required instance will work with the instance.
 *
 * @param <I>
 *            the instance type
 * @param <E>
 *            the secondary event type type
 */
@Documentation("Event fired after a instance has been canceled. The event will carry the instance that has been canceled."
		+ "<br><b>NOTE</b>: not all operations that required instance will work with the instance.")
public class AfterInstanceCancelEvent<I extends Instance, E extends TwoPhaseEvent>
		extends AbstractInstanceTwoPhaseEvent<I, E> {

	/**
	 * Instantiates a new after instance cancel event.
	 *
	 * @param instance
	 *            the instance
	 */
	public AfterInstanceCancelEvent(I instance) {
		super(instance);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected E createNextEvent() {
		return null;
	}

}
