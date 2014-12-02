package com.sirma.itt.emf.cache;

import com.sirma.itt.emf.util.Documentation;

/**
 * Cache expiration configuration.
 * 
 * @author BBonev
 */
public @interface Expiration {

	/**
	 * Maximum idle time a cache entry will be maintained in the cache, in milliseconds. If the idle
	 * time is exceeded, the entry will be expired cluster-wide. -1 means the entries never expire.
	 */
	@Documentation("Maximum idle time a cache entry will be maintained in the cache, in milliseconds. If the idle time is exceeded, the entry will be expired cluster-wide. -1 means the entries never expire.")
	long maxIdle() default -1L;

	/**
	 * Maximum lifespan of a cache entry, after which the entry is expired cluster-wide, in
	 * milliseconds. -1 means the entries never expire.
	 */
	@Documentation("Maximum lifespan of a cache entry, after which the entry is expired cluster-wide, in milliseconds. -1 means the entries never expire.")
	long lifespan() default -1L;

	/**
	 * Interval (in milliseconds) between subsequent runs to purge expired entries from memory and
	 * any cache stores. If you wish to disable the periodic eviction process altogether, set
	 * wakeupInterval to -1.
	 */
	@Documentation("Interval (in milliseconds) between subsequent runs to purge expired entries "
			+ "from memory and any cache stores. If you wish to disable the "
			+ "periodic eviction process altogether, set wakeupInterval to -1.")
	long interval() default 60000L;
}
