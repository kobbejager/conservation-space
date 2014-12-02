package com.sirma.itt.emf.executors;

/**
 * Enumeration of operations that identify the use of an instance in the context of
 * {@link ExecutableOperation#getDependencies(com.sirma.itt.emf.scheduler.SchedulerContext)} method.
 * 
 * @author BBonev
 */
public enum Operation {

	/**
	 * The instance will be created in the operation. Any other operation that depends on the
	 * current instance should wait until the creation is finished.
	 */
	CREATE,
	/** The operation will update properties on the mapped instance. */
	UPDATE,
	/**
	 * The operation will delete the instance identified by the current operation. Any operation
	 * that depend on the given instance should be executed before executing the current operation.
	 */
	DELETE,
	/**
	 * The instance will be used in read only manner. Operations that depend only on such instances
	 * could be executed in parallel to other operations but not before for one defined as DELETE.
	 */
	USE;
}
