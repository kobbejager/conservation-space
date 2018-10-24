/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.web.scripts;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.SEIPTenantIntegration;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.TenantAdminService;
import org.alfresco.repo.tenant.TenantDeployer;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TooBusyException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.surf.util.StringBuilderWriter;
import org.springframework.extensions.webscripts.AbstractRuntimeContainer;
import org.springframework.extensions.webscripts.Authenticator;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.Description.FormatStyle;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.Description.RequiredTransaction;
import org.springframework.extensions.webscripts.Description.RequiredTransactionParameters;
import org.springframework.extensions.webscripts.Description.TransactionCapability;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Registry;
import org.springframework.extensions.webscripts.Runtime;
import org.springframework.extensions.webscripts.ServerModel;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WrappingWebScriptRequest;
import org.springframework.extensions.webscripts.WrappingWebScriptResponse;
import org.springframework.util.FileCopyUtils;

/**
 * Repository (server-tier) container for Web Scripts.
 *
 * @author davidc
 */
public class RepositoryContainer extends AbstractRuntimeContainer implements TenantDeployer {
	// Logger
	protected static final Log logger = LogFactory.getLog(RepositoryContainer.class);

	/** Component Dependencies */
	private Repository repository;
	private RepositoryImageResolver imageResolver;
	private TransactionService transactionService;
	private RetryingTransactionHelper fallbackTransactionHelper;
	private AuthorityService authorityService;
	private DescriptorService descriptorService;
	private TenantAdminService tenantAdminService;
	private ObjectFactory registryFactory;
	private SimpleCache<String, Registry> webScriptsRegistryCache;
	private ReadWriteLock webScriptsRegistryLock = new ReentrantReadWriteLock();
	private boolean initialized;

	/**
	 * Sets the web scripts registry cache.
	 *
	 * @param webScriptsRegistryCache
	 *            the web scripts registry cache
	 */
	public void setWebScriptsRegistryCache(SimpleCache<String, Registry> webScriptsRegistryCache) {
		this.webScriptsRegistryCache = webScriptsRegistryCache;
	}

	/**
	 * Sets the registry factory.
	 *
	 * @param registryFactory
	 *            the new registry factory
	 */
	public void setRegistryFactory(ObjectFactory registryFactory) {
		this.registryFactory = registryFactory;
	}

	/**
	 * Sets the repository.
	 *
	 * @param repository
	 *            the new repository
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * Sets the repository image resolver.
	 *
	 * @param imageResolver
	 *            the new repository image resolver
	 */
	public void setRepositoryImageResolver(RepositoryImageResolver imageResolver) {
		this.imageResolver = imageResolver;
	}

	/**
	 * Sets the transaction service.
	 *
	 * @param transactionService
	 *            the new transaction service
	 */
	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	/**
	 * Sets the fallback transaction helper.
	 *
	 * @param fallbackTransactionHelper
	 *            an unlimited transaction helper used to generate error
	 *            responses
	 */
	public void setFallbackTransactionHelper(RetryingTransactionHelper fallbackTransactionHelper) {
		this.fallbackTransactionHelper = fallbackTransactionHelper;
	}

	/**
	 * Sets the descriptor service.
	 *
	 * @param descriptorService
	 *            the new descriptor service
	 */
	public void setDescriptorService(DescriptorService descriptorService) {
		this.descriptorService = descriptorService;
	}

	/**
	 * Sets the authority service.
	 *
	 * @param authorityService
	 *            the new authority service
	 */
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	/**
	 * Sets the tenant admin service.
	 *
	 * @param tenantAdminService
	 *            the new tenant admin service
	 */
	public void setTenantAdminService(TenantAdminService tenantAdminService) {
		this.tenantAdminService = tenantAdminService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.web.scripts.Container#getDescription()
	 */
	/**
	 * {@inheritDoc}
	 */
	public ServerModel getDescription() {
		return new RepositoryServerModel(descriptorService.getCurrentRepositoryDescriptor(),
				descriptorService.getServerDescriptor());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.alfresco.web.scripts.AbstractRuntimeContainer#getScriptParameters()
	 */
	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getScriptParameters() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.putAll(super.getScriptParameters());
		addRepoParameters(params);
		return params;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.alfresco.web.scripts.AbstractRuntimeContainer#getTemplateParameters()
	 */
	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getTemplateParameters() {
		// Ensure we have a transaction - we might be generating the status
		// template after the main transaction failed
		return fallbackTransactionHelper.doInTransaction(new RetryingTransactionCallback<Map<String, Object>>() {
			public Map<String, Object> execute() throws Throwable {
				Map<String, Object> params = new HashMap<String, Object>();
				params.putAll(RepositoryContainer.super.getTemplateParameters());
				params.put(TemplateService.KEY_IMAGE_RESOLVER, imageResolver.getImageResolver());
				addRepoParameters(params);
				return params;
			}
		}, true);
	}

	/**
	 * Add Repository specific parameters.
	 *
	 * @param params
	 *            the params
	 */
	private void addRepoParameters(Map<String, Object> params) {
		if (AlfrescoTransactionSupport.getTransactionId() != null
				&& AuthenticationUtil.getFullAuthentication() != null) {
			NodeRef rootHome = repository.getRootHome();
			if (rootHome != null) {
				params.put("roothome", rootHome);
			}
			NodeRef companyHome = repository.getCompanyHome();
			if (companyHome != null) {
				params.put("companyhome", companyHome);
			}
			NodeRef person = repository.getFullyAuthenticatedPerson();
			if (person != null) {
				params.put("person", person);
				NodeRef userHome = repository.getUserHome(person);
				if (userHome != null) {
					params.put("userhome", userHome);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.alfresco.web.scripts.RuntimeContainer#executeScript(org.alfresco.web.
	 * scripts.WebScriptRequest, org.alfresco.web.scripts.WebScriptResponse,
	 * org.alfresco.web.scripts.Authenticator)
	 */
	/**
	 * {@inheritDoc}
	 */
	public void executeScript(WebScriptRequest scriptReq, WebScriptResponse scriptRes, final Authenticator auth)
			throws IOException {
		final boolean debug = logger.isDebugEnabled();
		final WebScript script = scriptReq.getServiceMatch().getWebScript();
		final Description desc = script.getDescription();

		// Escalate the webscript declared level of authentication to the
		// container required authentication
		// eg. must be guest if MT is enabled unless credentials are empty
		RequiredAuthentication containerRequiredAuthentication = getRequiredAuthentication();
		final RequiredAuthentication required = (desc.getRequiredAuthentication()
				.compareTo(containerRequiredAuthentication) < 0 && !auth.emptyCredentials()
						? containerRequiredAuthentication : desc.getRequiredAuthentication());
		final boolean isGuest = scriptReq.isGuest();

		if (required == RequiredAuthentication.none) {
			// TODO revisit - cleared here, in-lieu of WebClient clear
			AuthenticationUtil.clearCurrentSecurityContext();

			transactionedExecuteAs(script, scriptReq, scriptRes);
		} else if ((required == RequiredAuthentication.user || required == RequiredAuthentication.admin) && isGuest) {
			throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "Web Script " + desc.getId()
					+ " requires user authentication; however, a guest has attempted access.");
		} else {
			String currentUser = null;
			try {
				AuthenticationUtil.pushAuthentication();

				//
				// Determine if user already authenticated
				//
				if (debug) {
					currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
					logger.debug("Current authentication: "
							+ (currentUser == null ? "unauthenticated" : "authenticated as " + currentUser));
					logger.debug("Authentication required: " + required);
					logger.debug("Guest login requested: " + isGuest);
				}

				//
				// Apply appropriate authentication to Web Script invocation
				//
				RetryingTransactionCallback<Boolean> authWork = new RetryingTransactionCallback<Boolean>() {
					public Boolean execute() throws Exception {
						if (auth == null || auth.authenticate(required, isGuest)) {
							// The user will now have been authenticated, based
							// on HTTP Auth, Ticket etc
							// Check that the user they authenticated as has
							// appropriate access to the script

							// Check to see if they supplied HTTP Auth or Ticket
							// as guest, on a script that needs more
							if (required == RequiredAuthentication.user || required == RequiredAuthentication.admin) {
								String authenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
								String runAsUser = AuthenticationUtil.getRunAsUser();

								if ((authenticatedUser == null)
										|| (authenticatedUser.equals(runAsUser) && authorityService.hasGuestAuthority())
										|| (!authenticatedUser.equals(runAsUser)
												&& authorityService.isGuestAuthority(authenticatedUser))) {
									throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "Web Script "
											+ desc.getId()
											+ " requires user authentication; however, a guest has attempted access.");
								}
							}

							// Check to see if they're admin or system on an
							// Admin only script
							if (required == RequiredAuthentication.admin && !(authorityService.hasAdminAuthority())
									&& !AuthenticationUtil.getFullyAuthenticatedUser()
											.equalsIgnoreCase(SEIPTenantIntegration.getSystemUser())) {
								throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "Web Script "
										+ desc.getId()
										+ " requires admin authentication; however, a non-admin has attempted access.");
							}

							if (debug) {
								String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
								logger.debug("Authentication: " + (currentUser == null ? "unauthenticated"
										: "authenticated as " + currentUser));
							}

							return true;
						}
						return false;
					}
				};

				boolean readOnly = transactionService.isReadOnly();
				boolean requiresNew = !readOnly
						&& AlfrescoTransactionSupport.getTransactionReadState() == TxnReadState.TXN_READ_ONLY;
				if (transactionService.getRetryingTransactionHelper().doInTransaction(authWork, readOnly,
						requiresNew)) {
					// Execute Web Script if authentication passed
					// The Web Script has its own txn management with potential
					// runAs() user
					transactionedExecuteAs(script, scriptReq, scriptRes);
				}
			} finally {
				//
				// Reset authentication for current thread
				//
				try {
					AuthenticationUtil.popAuthentication();
				} catch (Exception e) {
					logger.error("Faield to restore authentication context! " + currentUser + "! " + e.getMessage());
				}

				if (debug) {
					currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
					logger.debug("Authentication reset: "
							+ (currentUser == null ? "unauthenticated" : "authenticated as " + currentUser));
				}
			}
		}
	}

	/**
	 * Execute script within required level of transaction.
	 *
	 * @param script
	 *            the script
	 * @param scriptReq
	 *            the script req
	 * @param scriptRes
	 *            the script res
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected void transactionedExecute(final WebScript script, final WebScriptRequest scriptReq,
			final WebScriptResponse scriptRes) throws IOException {
		final Description description = script.getDescription();
		if (description.getRequiredTransaction() == RequiredTransaction.none) {
			script.execute(scriptReq, scriptRes);
		} else {
			final BufferedRequest bufferedReq;
			final BufferedResponse bufferedRes;
			RequiredTransactionParameters trxParams = description.getRequiredTransactionParameters();
			if (trxParams.getCapability() == TransactionCapability.readwrite) {
				if (trxParams.getBufferSize() > 0) {
					if (logger.isDebugEnabled())
						logger.debug("Creating Transactional Response for ReadWrite transaction; buffersize="
								+ trxParams.getBufferSize());

					// create buffered request and response that allow
					// transaction retrying
					bufferedReq = new BufferedRequest(scriptReq);
					bufferedRes = new BufferedResponse(scriptRes, trxParams.getBufferSize());
				} else {
					if (logger.isDebugEnabled())
						logger.debug("Transactional Response bypassed for ReadWrite - buffersize=0");
					bufferedReq = null;
					bufferedRes = null;
				}
			} else {
				bufferedReq = null;
				bufferedRes = null;
			}

			// encapsulate script within transaction
			RetryingTransactionCallback<Object> work = new RetryingTransactionCallback<Object>() {
				public Object execute() throws Exception {
					try {
						if (logger.isDebugEnabled())
							logger.debug("Begin retry transaction block: " + description.getRequiredTransaction() + ","
									+ description.getRequiredTransactionParameters().getCapability());

						if (bufferedRes == null) {
							script.execute(scriptReq, scriptRes);
						} else {
							// Reset the request and response in case of a
							// transaction retry
							bufferedReq.reset();
							bufferedRes.reset();
							script.execute(bufferedReq, bufferedRes);
						}
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("Transaction exception: " + description.getRequiredTransaction() + ": "
									+ e.getMessage());
							// Note: user transaction shouldn't be null, but
							// just in case inside this exception handler
							UserTransaction userTrx = RetryingTransactionHelper.getActiveUserTransaction();
							if (userTrx != null) {
								logger.debug("Transaction status: " + userTrx.getStatus());
							}
						}

						UserTransaction userTrx = RetryingTransactionHelper.getActiveUserTransaction();
						if (userTrx != null) {
							if (userTrx.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
								if (logger.isDebugEnabled())
									logger.debug("Marking web script transaction for rollback");
								try {
									userTrx.setRollbackOnly();
								} catch (Exception re) {
									if (logger.isDebugEnabled())
										logger.debug("Caught and ignoring exception during marking for rollback: "
												+ re.getMessage());
								}
							}
						}

						// re-throw original exception for retry
						throw e;
					} finally {
						if (logger.isDebugEnabled())
							logger.debug("End retry transaction block: " + description.getRequiredTransaction() + ","
									+ description.getRequiredTransactionParameters().getCapability());
					}

					return null;
				}
			};

			boolean readonly = description.getRequiredTransactionParameters()
					.getCapability() == TransactionCapability.readonly;
			boolean requiresNew = description.getRequiredTransaction() == RequiredTransaction.requiresnew;

			// log a warning if we detect a GET webscript being run in a
			// readwrite transaction, GET calls should
			// NOT have any side effects so this scenario as a warning sign
			// something maybe amiss, see ALF-10179.
			if (logger.isDebugEnabled() && !readonly && "GET".equalsIgnoreCase(description.getMethod())) {
				logger.debug("Webscript with URL '" + scriptReq.getURL()
						+ "' is a GET request but it's descriptor has declared a readwrite transaction is required");
			}

			try {
				transactionService.getRetryingTransactionHelper().doInTransaction(work, readonly, requiresNew);
			} catch (TooBusyException e) {
				// Map TooBusyException to a 503 status code
				throw new WebScriptException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage(), e);
			} finally {
				// Get rid of any temporary files
				if (bufferedReq != null) {
					bufferedReq.close();
				}
			}

			// Ensure a response is always flushed after successful execution
			if (bufferedRes != null) {
				bufferedRes.writeResponse();
			}

		}
	}

	/**
	 * Execute script within required level of transaction as required effective
	 * user.
	 *
	 * @param script
	 *            the script
	 * @param scriptReq
	 *            the script req
	 * @param scriptRes
	 *            the script res
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void transactionedExecuteAs(final WebScript script, final WebScriptRequest scriptReq,
			final WebScriptResponse scriptRes) throws IOException {
		String runAs = script.getDescription().getRunAs();
		if (runAs == null) {
			transactionedExecute(script, scriptReq, scriptRes);
		} else {
			RunAsWork<Object> work = new RunAsWork<Object>() {
				public Object doWork() throws Exception {
					transactionedExecute(script, scriptReq, scriptRes);
					return null;
				}
			};
			AuthenticationUtil.runAs(work, runAs);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.web.scripts.AbstractRuntimeContainer#getRegistry()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Registry getRegistry() {
		String tenantDomain = tenantAdminService.getCurrentUserDomain();
		Registry registry;
		webScriptsRegistryLock.readLock().lock();
		try {
			registry = webScriptsRegistryCache.get(tenantDomain);
		} finally {
			webScriptsRegistryLock.readLock().unlock();
		}
		if (registry == null) {
			webScriptsRegistryLock.writeLock().lock();
			try {
				// Double check now we have write lock
				registry = webScriptsRegistryCache.get(tenantDomain);

				// Initialize / reinitialize the registry in this thread only
				if (registry == null) {
					registry = (Registry) registryFactory.getObject();
					// We only need to reset the registry if the superclass
					// thinks its already initialized
					if (initialized) {
						registry.reset();
					}
					webScriptsRegistryCache.put(tenantDomain, registry);
				}
			} finally {
				webScriptsRegistryLock.writeLock().unlock();
			}
		}
		return registry;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.alfresco.web.scripts.AbstractRuntimeContainer#onApplicationEvent(org.
	 * springframework.context.ApplicationEvent)
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			ContextRefreshedEvent refreshEvent = (ContextRefreshedEvent) event;
			ApplicationContext refreshContext = refreshEvent.getApplicationContext();
			if (refreshContext != null && refreshContext.equals(applicationContext)) {
				RunAsWork<Object> work = new RunAsWork<Object>() {
					public Object doWork() throws Exception {
						reset();
						return null;
					}
				};
				AuthenticationUtil.runAs(work, AuthenticationUtil.getSystemUserName());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.web.scripts.AbstractRuntimeContainer#
	 * getRequiredAuthentication()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RequiredAuthentication getRequiredAuthentication() {
		if (AuthenticationUtil.isMtEnabled()) {
			return RequiredAuthentication.guest; // user or guest (ie. at least
													// guest)
		}

		return RequiredAuthentication.none;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.alfresco.web.scripts.RuntimeContainer#authenticate(org.alfresco.web.
	 * scripts.Authenticator,
	 * org.alfresco.web.scripts.Description.RequiredAuthentication)
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean authenticate(Authenticator auth, RequiredAuthentication required) {
		if (auth != null) {
			AuthenticationUtil.clearCurrentSecurityContext();

			return auth.authenticate(required, false);
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.web.scripts.AbstractRuntimeContainer#reset()
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
			public Object execute() throws Exception {
				destroy();
				init();

				return null;
			}
		}, true, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.repo.tenant.TenantDeployer#onEnableTenant()
	 */
	/**
	 * {@inheritDoc}
	 */
	public void onEnableTenant() {
		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.repo.tenant.TenantDeployer#onDisableTenant()
	 */
	/**
	 * {@inheritDoc}
	 */
	public void onDisableTenant() {
		destroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.repo.tenant.TenantDeployer#init()
	 */
	/**
	 * {@inheritDoc}
	 */
	public void init() {
		tenantAdminService.register(this);

		super.reset();

		initialized = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.alfresco.repo.tenant.TenantDeployer#destroy()
	 */
	/**
	 * {@inheritDoc}
	 */
	public void destroy() {
		try {
			webScriptsRegistryLock.writeLock().lock();
			webScriptsRegistryCache.remove(tenantAdminService.getCurrentUserDomain());

			if (logger.isTraceEnabled()) {
				Exception e = new Exception("RepositoryContainer destroy called.");
				e.fillInStackTrace();
				logger.trace("", e);
			}
		} finally {
			webScriptsRegistryLock.writeLock().unlock();
		}

		initialized = false;
	}

	/**
	 * Transactional Buffered Response.
	 */
	private static class BufferedResponse implements WrappingWebScriptResponse {
		private WebScriptResponse res;
		private int bufferSize;
		private ByteArrayOutputStream outputStream = null;
		private StringBuilderWriter outputWriter = null;

		/**
		 * Construct.
		 *
		 * @param res
		 *            the res
		 * @param bufferSize
		 *            the buffer size
		 */
		public BufferedResponse(WebScriptResponse res, int bufferSize) {
			this.res = res;
			this.bufferSize = bufferSize;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WrappingWebScriptResponse#getNext()
		 */
		/**
		 * {@inheritDoc}
		 */
		public WebScriptResponse getNext() {
			return res;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#addHeader(java.lang.
		 * String, java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public void addHeader(String name, String value) {
			res.addHeader(name, value);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.alfresco.web.scripts.WebScriptResponse#encodeScriptUrl(java.lang.
		 * String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public String encodeScriptUrl(String url) {
			return res.encodeScriptUrl(url);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.alfresco.web.scripts.WebScriptResponse#getEncodeScriptUrlFunction
		 * (java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public String getEncodeScriptUrlFunction(String name) {
			return res.getEncodeScriptUrlFunction(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptResponse#
		 * encodeResourceUrl(java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public String encodeResourceUrl(String url) {
			return res.encodeResourceUrl(url);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptResponse#
		 * getEncodeResourceUrlFunction(java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public String getEncodeResourceUrlFunction(String name) {
			return res.getEncodeResourceUrlFunction(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#getOutputStream()
		 */
		/**
		 * {@inheritDoc}
		 */
		public OutputStream getOutputStream() throws IOException {
			if (outputStream == null) {
				if (outputWriter != null) {
					throw new AlfrescoRuntimeException("Already buffering output writer");
				}
				this.outputStream = new ByteArrayOutputStream(bufferSize);
			}
			return outputStream;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#getRuntime()
		 */
		/**
		 * {@inheritDoc}
		 */
		public Runtime getRuntime() {
			return res.getRuntime();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#getWriter()
		 */
		/**
		 * {@inheritDoc}
		 */
		public Writer getWriter() throws IOException {
			if (outputWriter == null) {
				if (outputStream != null) {
					throw new AlfrescoRuntimeException("Already buffering output stream");
				}
				outputWriter = new StringBuilderWriter(bufferSize);
			}
			return outputWriter;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#reset()
		 */
		/**
		 * {@inheritDoc}
		 */
		public void reset() {
			if (outputStream != null) {
				outputStream.reset();
			} else if (outputWriter != null) {
				outputWriter = null;
			}
			res.reset();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.alfresco.web.scripts.WebScriptResponse#setCache(org.alfresco.web.
		 * scripts.Cache)
		 */
		/**
		 * {@inheritDoc}
		 */
		public void setCache(Cache cache) {
			res.setCache(cache);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.alfresco.web.scripts.WebScriptResponse#setContentType(java.lang.
		 * String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public void setContentType(String contentType) {
			res.setContentType(contentType);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.alfresco.web.scripts.WebScriptResponse#setContentEncoding(java.
		 * lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public void setContentEncoding(String contentEncoding) {
			res.setContentEncoding(contentEncoding);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#setHeader(java.lang.
		 * String, java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		public void setHeader(String name, String value) {
			res.setHeader(name, value);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.alfresco.web.scripts.WebScriptResponse#setStatus(int)
		 */
		/**
		 * {@inheritDoc}
		 */
		public void setStatus(int status) {
			res.setStatus(status);
		}

		/**
		 * Write buffered response to underlying response.
		 */
		private void writeResponse() {
			try {
				if (logger.isDebugEnabled() && outputStream != null) {
					logger.debug("Writing Transactional response: size=" + outputStream.size());
				}

				if (outputWriter != null) {
					outputWriter.flush();
					res.getWriter().write(outputWriter.toString());
				} else if (outputStream != null) {
					if (logger.isDebugEnabled())
						logger.debug("Writing Transactional response: size=" + outputStream.size());

					outputStream.flush();
					outputStream.writeTo(res.getOutputStream());
				}
			} catch (IOException e) {
				throw new AlfrescoRuntimeException("Failed to commit buffered response", e);
			}
		}
	}

	/**
	 * The Class BufferedRequest.
	 */
	private static class BufferedRequest implements WrappingWebScriptRequest {
		private WebScriptRequest req;
		private File requestBody;
		private InputStream contentStream;
		private BufferedReader contentReader;

		/**
		 * Instantiates a new buffered request.
		 *
		 * @param req
		 *            the req
		 */
		public BufferedRequest(WebScriptRequest req) {
			this.req = req;
		}

		// If a web script wants access to the request stream, we copy it to a
		// temporary file, so we can have several
		// retries at the transaction
		/**
		 * Gets the request body as file.
		 *
		 * @return the request body as file
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		private File getRequestBodyAsFile() throws IOException {
			if (this.requestBody == null) {
				this.requestBody = TempFileProvider.createTempFile("webscript_", ".bin");
				OutputStream out = new FileOutputStream(this.requestBody);
				FileCopyUtils.copy(req.getContent().getInputStream(), out);
			}
			return this.requestBody;
		}

		/**
		 * Reset.
		 */
		public void reset() {
			if (contentStream != null) {
				try {
					contentStream.close();
				} catch (Exception e) {
				}
				contentStream = null;
			}
			if (contentReader != null) {
				try {
					contentReader.close();
				} catch (Exception e) {
				}
				contentReader = null;
			}
		}

		/**
		 * Close.
		 */
		public void close() {
			reset();
			if (requestBody != null) {
				try {
					requestBody.delete();
				} catch (Exception e) {
				}
				requestBody = null;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WrappingWebScriptRequest#
		 * getNext()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public WebScriptRequest getNext() {
			return req;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * forceSuccessStatus()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean forceSuccessStatus() {
			return req.forceSuccessStatus();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#getAgent()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAgent() {
			return req.getAgent();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#getContent
		 * ()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Content getContent() {
			final Content wrapped = req.getContent();
			return new Content() {

				@Override
				public String getContent() throws IOException {
					return wrapped.getContent();
				}

				@Override
				public String getEncoding() {
					return wrapped.getEncoding();
				}

				@Override
				public String getMimetype() {
					return wrapped.getMimetype();
				}

				@Override
				public long getSize() {
					return wrapped.getSize();
				}

				@Override
				public InputStream getInputStream() {
					if (BufferedRequest.this.contentReader != null) {
						throw new IllegalStateException("Reader in use");
					}
					if (BufferedRequest.this.contentStream == null) {
						try {
							BufferedRequest.this.contentStream = new FileInputStream(getRequestBodyAsFile());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					return BufferedRequest.this.contentStream;
				}

				@Override
				public BufferedReader getReader() throws IOException {
					if (BufferedRequest.this.contentStream != null) {
						throw new IllegalStateException("Input Stream in use");
					}
					if (BufferedRequest.this.contentReader == null) {
						String encoding = wrapped.getEncoding();
						BufferedRequest.this.contentReader = new BufferedReader(
								new InputStreamReader(new FileInputStream(getRequestBodyAsFile()),
										encoding == null ? "ISO-8859-1" : encoding));
					}
					return BufferedRequest.this.contentReader;
				}
			};
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getContentType()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getContentType() {
			return req.getContentType();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getContextPath()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getContextPath() {
			return req.getContextPath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getExtensionPath()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getExtensionPath() {
			return req.getExtensionPath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#getFormat(
		 * )
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getFormat() {
			return req.getFormat();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getFormatStyle()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public FormatStyle getFormatStyle() {
			return req.getFormatStyle();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#getHeader(
		 * java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getHeader(String name) {
			return req.getHeader(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getHeaderNames()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String[] getHeaderNames() {
			return req.getHeaderNames();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getHeaderValues(java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String[] getHeaderValues(String name) {
			return req.getHeaderValues(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getJSONCallback()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getJSONCallback() {
			return req.getJSONCallback();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getParameter(java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getParameter(String name) {
			return req.getParameter(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getParameterNames()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String[] getParameterNames() {
			return req.getParameterNames();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getParameterValues(java.lang.String)
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String[] getParameterValues(String name) {
			return req.getParameterValues(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getPathInfo()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPathInfo() {
			return req.getPathInfo();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getQueryString()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getQueryString() {
			return req.getQueryString();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#getRuntime
		 * ()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Runtime getRuntime() {
			return req.getRuntime();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getServerPath()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getServerPath() {
			return req.getServerPath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getServiceContextPath()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getServiceContextPath() {
			return req.getServiceContextPath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getServiceMatch()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Match getServiceMatch() {
			return req.getServiceMatch();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * getServicePath()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getServicePath() {
			return req.getServicePath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#getURL()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getURL() {
			return req.getURL();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.extensions.webscripts.WebScriptRequest#isGuest()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isGuest() {
			return req.isGuest();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.extensions.webscripts.WebScriptRequest#
		 * parseContent()
		 */
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object parseContent() {
			return req.parseContent();
		}
	}
}
