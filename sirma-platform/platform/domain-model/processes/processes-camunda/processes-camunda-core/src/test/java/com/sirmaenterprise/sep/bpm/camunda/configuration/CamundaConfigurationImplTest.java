package com.sirmaenterprise.sep.bpm.camunda.configuration;

import static com.sirmaenterprise.sep.bpm.camunda.MockProvider.DEFAULT_ENGINE;
import static com.sirmaenterprise.sep.bpm.camunda.util.ProcessEngineUtil.getTenantId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sirma.itt.seip.configuration.ConfigurationException;
import com.sirma.itt.seip.configuration.convert.ConverterContext;
import com.sirma.itt.seip.configuration.convert.GroupConverterContext;
import com.sirma.itt.seip.security.context.SecurityContext;

/**
 * Tests {@link CamundaConfigurationImpl}
 * 
 * @author bbanchev
 */
public class CamundaConfigurationImplTest {
	@Mock
	private SecurityContext securityContext;
	@Mock
	private ConverterContext converterContext;
	@Mock
	private GroupConverterContext groupConverterContext;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(securityContext.isActive()).thenReturn(Boolean.TRUE);
		when(securityContext.getCurrentTenantId()).thenReturn(getTenantId(DEFAULT_ENGINE));
		when(securityContext.isSystemTenant()).thenReturn(Boolean.FALSE);
	}

	@Test(expected = ConfigurationException.class)
	public void testBuildDatasourceNameWithInvalidContext() throws Exception {
		when(securityContext.getCurrentTenantId()).thenReturn(SecurityContext.SYSTEM_TENANT);
		when(securityContext.isSystemTenant()).thenReturn(Boolean.TRUE);

		CamundaConfigurationImpl.buildDatasourceName(converterContext, securityContext);
	}

	/**
	 * Test method for
	 * {@link com.sirmaenterprise.sep.bpm.camunda.configuration.CamundaConfigurationImpl#buildDatasourceName(com.sirma.itt.seip.configuration.convert.ConverterContext, com.sirma.itt.seip.security.context.SecurityContext)}.
	 */
	@Test
	public void testBuildDatasourceName() throws Exception {
		String buildDatasourceName = CamundaConfigurationImpl.buildDatasourceName(converterContext, securityContext);
		assertNotNull(buildDatasourceName);
		assertEquals(getTenantId(DEFAULT_ENGINE) + "_camunda", buildDatasourceName);
	}

	/**
	 * Test method for
	 * {@link com.sirmaenterprise.sep.bpm.camunda.configuration.CamundaConfigurationImpl#buildEngineName(com.sirma.itt.seip.configuration.convert.ConverterContext, com.sirma.itt.seip.security.context.SecurityContext)}.
	 */
	@Test
	public void testBuildEngineName() throws Exception {
		String engineName = CamundaConfigurationImpl.buildEngineName(converterContext, securityContext);
		assertNotNull(engineName);
		assertEquals(DEFAULT_ENGINE, engineName);
	}

}
