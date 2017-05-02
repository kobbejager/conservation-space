package com.sirma.itt.emf.authentication.session;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.seip.StringPair;
import com.sirma.itt.seip.collections.ContextualMap;
import com.sirma.itt.seip.configuration.annotation.ConfigurationPropertyDefinition;
import com.sirma.itt.seip.resources.User;
import com.sirma.itt.seip.security.UserPreferences;
import com.sirma.itt.seip.security.annotation.RunAsSystem;
import com.sirma.itt.seip.security.util.SecurityUtil;
import com.sirma.itt.seip.tasks.Schedule;

/**
 * Manager is responsible to synchronize and organize logout process during concurrent invocations.<br>
 * TODO: refactor to support per user user sessions and to remove dependency from web sessions. <br>
 * TODO: add multi session per user
 *
 * @author bbanchev
 */
@ApplicationScoped
public class SessionManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/** PILCROW SIGN. */
	private static final String DELIMITER = "\u00B6";

	/** The use multi browser mode. */
	private static final boolean MULTI_BROWSER_USER_MODE = true;

	/** List of currently processed users. */
	private final Set<String> preprocessing = new HashSet<>();

	private final Timer logoutTimer = new Timer();

	private Map<HttpSession, StringPair> users;

	@Inject
	private ContextualMap<String, HttpSession> sessions;
	@Inject
	private UserPreferences userPreferences;

	/**
	 * Inits the logged in users info timer.
	 */
	@PostConstruct
	public void init() {
		sessions.initializeWith(WeakHashMap::new);
		users = new LinkedHashMap<>();
	}

	@Schedule
	@RunAsSystem(protectCurrentTenant = false)
	@ConfigurationPropertyDefinition(name = "security.info.loggedusers.rate", defaultValue = "0 0/15 * ? * *", sensitive = true, system = true, label = "Timer rate in minutes to display the current logged in user. Performs also user session cleanup")
	void logTimeout() {
		synchronized (users) {
			long timeout = userPreferences.getSessionTimeout() * 60L * 1000L;
			Iterator<Entry<HttpSession, StringPair>> iterator = users.entrySet().iterator();
			long now = new Date().getTime();
			while (iterator.hasNext()) {
				Map.Entry<HttpSession, StringPair> entry = iterator.next();
				try {
					if (now - entry.getKey().getLastAccessedTime() >= timeout) {
						LOGGER.info("Automatically removed user '{}' due to inactivity", entry.getValue());
						iterator.remove();
					}
				} catch (IllegalStateException e) {
					LOGGER.info("Removing user due to {}", e.getMessage());
					LOGGER.trace(e.getMessage(), e);
					iterator.remove();
				}
			}
			Map<String, List<StringPair>> tenantList = new HashMap<>();
			for (StringPair pair : users.values()) {
				StringPair userAndTenant = SecurityUtil.getUserAndTenant(pair.getFirst());
				String tenantId = userAndTenant.getSecond();
				if (!tenantList.containsKey(tenantId)) {
					tenantList.put(tenantId, new LinkedList<>());
				}
				tenantList.get(tenantId).add(pair);
			}
			tenantList.entrySet().stream().forEach(
					s -> LOGGER.info("List of currently logged users: {} in '{}'", s.getValue(), s.getKey()));
			LOGGER.info("List of currently logged users: {}", users.values());
		}
	}

	/**
	 * On shutdown terminates all timers.
	 */
	@PreDestroy
	public void onShutdown() {
		logoutTimer.cancel();
	}

	/**
	 * Invoke the user on logout started process. 5 sec after invoke user is removed automatically.
	 *
	 * @param userId
	 *            is the user id.
	 */
	public void beginLogout(String userId) {
		if (userId != null) {
			synchronized (preprocessing) {
				boolean processing = isProcessing(userId);
				if (!processing) {
					preprocessing.add(userId);
					// start the process and remove the processing id, since it
					// is expected all tabs
					// had finished the logout initiating process
					logoutTimer.schedule(new RemoveCachedProcess(userId), 5000L);
				}
			}
		}
	}

	/**
	 * Is the user currently processed.
	 *
	 * @param userId
	 *            is the user id
	 * @return true if it is still in local cache, false if user is not in cache
	 */
	public boolean isProcessing(String userId) {
		if (userId != null) {
			synchronized (preprocessing) {
				if (preprocessing.contains(userId)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Explicitly remove user from cache.
	 *
	 * @param userId
	 *            is the user id
	 */
	public void finishLogout(String userId) {
		if (userId != null) {
			synchronized (preprocessing) {
				logoutTimer.schedule(new RemoveCachedProcess(userId), 2000L);
			}
		}
	}

	/**
	 * Register a session to its idp index. If the same index exists it is replaced and the old value is returned
	 *
	 * @param sessionIndex
	 *            the session index
	 * @param session
	 *            the session to link
	 * @return null if the value is not added or if the index has not been presented in the register
	 */
	public HttpSession registerSession(String sessionIndex, HttpSession session) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Register sessionId:" + sessionIndex + " to: " + session);
		}
		if (sessionIndex != null && session != null) {
			return sessions.put(sessionIndex, session);
		}
		return null;
	}

	/**
	 * Gets the session for the given index id.
	 *
	 * @param sessionIndex
	 *            the session index
	 * @return the session for the key or null if not found or sessionIndex is null
	 */
	public HttpSession getSession(String sessionIndex) {
		return sessionIndex != null ? sessions.get(sessionIndex) : null;
	}

	/**
	 * Unregister session from the register.
	 *
	 * @param sessionIndex
	 *            the session index
	 * @return the session that has been linked to the key or null if not found or sessionIndex is null
	 */
	public HttpSession unregisterSession(String sessionIndex) {
		return sessionIndex != null ? sessions.remove(sessionIndex) : null;
	}

	/**
	 * Gets a unique identifier based on the request and current user.
	 *
	 * @param currentUser
	 *            is the current user if null - null is returned
	 * @param request
	 *            is the request for servlet
	 * @return the id for the user or/and browser
	 */
	public String getClientId(User currentUser, HttpServletRequest request) {
		if (!MULTI_BROWSER_USER_MODE) {
			return request.getSession().getId();
		}
		String header = request.getHeader("User-Agent");
		return header + DELIMITER + request.getSession().getId();
	}

	/**
	 * On login user add it to the tracking list of users. Store the session and user info. Removing information after
	 * timeout period.
	 *
	 * @param session
	 *            the session to register
	 * @param userId
	 *            the user id
	 * @param clientId
	 *            the client id (browser info)
	 */
	public void trackUser(HttpSession session, String userId, String clientId) {
		if (userId == null || clientId == null || session == null) {
			LOGGER.warn("Missing required arguments: session (" + session + "), username (" + userId
					+ ") client identificator (" + clientId + ")");
		}

		synchronized (users) {
			StringPair loginInfo = new StringPair(userId, clientId);
			if (session != null) {
				LOGGER.trace("Adding logged-in user: '" + loginInfo + "' and session id:" + session.getId());
				users.put(session, loginInfo);
			}
		}
	}

	/**
	 * On logout user method should be invoked on session invalidation or logging out user.
	 *
	 * @param session
	 *            the session to remove
	 */
	public void untrackUser(HttpSession session) {
		if (session != null) {
			synchronized (users) {
				StringPair removed = users.remove(session);
				LOGGER.trace("Removing logged-out user: '" + (removed == null ? "-" : removed) + "' and session id:"
						+ session.getId());
			}
		}
	}

	/**
	 * Thread to remove from cache.
	 *
	 * @author bbanchev
	 */
	private final class RemoveCachedProcess extends TimerTask {

		// local copy if id
		private final String userId;

		/**
		 * Construct the thread for that user.
		 *
		 * @param userId
		 *            is the user id.
		 */
		public RemoveCachedProcess(String userId) {
			this.userId = userId;
		}

		@Override
		public void run() {
			synchronized (preprocessing) {
				preprocessing.remove(userId);
			}
		}
	}
}
