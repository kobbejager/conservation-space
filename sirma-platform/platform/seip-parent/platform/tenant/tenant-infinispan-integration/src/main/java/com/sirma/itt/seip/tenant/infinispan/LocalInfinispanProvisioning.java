package com.sirma.itt.seip.tenant.infinispan;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.sirma.itt.seip.cache.CacheConfiguration;
import com.sirma.itt.seip.cache.CacheConfigurationProvider;
import com.sirma.itt.seip.cache.CacheRegister;
import com.sirma.itt.seip.security.context.SecurityContextManager;
import com.sirma.itt.seip.tenant.context.TenantInfo;

/**
 * Infinispan provisioning for local cache, configured via Jboss CLI.
 *
 * @author BBonev
 */
@ApplicationScoped
public class LocalInfinispanProvisioning {

	@Inject
	private CacheConfigurationProvider configurationExtension;

	@Inject
	private CacheRegister cacheRegister;

	@Inject
	private SecurityContextManager contextManager;

	/**
	 * Provision the infinispan caches.
	 *
	 * @param tenantInfo
	 *            the tenant info
	 */
	public void provision(TenantInfo tenantInfo) {
		Collection<CacheConfiguration> configurations = configurationExtension.getConfigurations().values();

		contextManager
			.executeAsTenant(tenantInfo.getTenantId())
				.predicate(cacheRegister::registerCaches, configurations);
	}

	/**
	 * Delete the caches.
	 *
	 * @param tenantInfo
	 *            the tenant info
	 */
	public void deleteCaches(TenantInfo tenantInfo) {
		contextManager
			.executeAsTenant(tenantInfo.getTenantId())
				.predicate(cacheRegister::unregisterCaches, getCacheNames());
	}

	private List<String> getCacheNames() {
		return configurationExtension
				.getConfigurations()
					.values()
					.stream()
					.map(CacheConfiguration::name)
					.collect(Collectors.toList());
	}
}