package com.sirma.itt.emf.resources.event;

import com.sirma.itt.emf.event.AbstractInstanceEvent;
import com.sirma.itt.emf.resources.model.Resource;
import com.sirma.itt.emf.util.Documentation;

/**
 * Event fired when new resource has been added to the system.
 * 
 * @author BBonev
 */
@Documentation("Event fired when new resource has been added to the system.")
public class ResourceAddedEvent extends AbstractInstanceEvent<Resource> {

	/**
	 * Instantiates a new resource added event.
	 * 
	 * @param instance
	 *            the resource instance
	 */
	public ResourceAddedEvent(Resource instance) {
		super(instance);
	}

}
