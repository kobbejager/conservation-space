package com.sirma.itt.emf.authentication.session;

import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * The listener interface for receiving session related events.  On session destruction loggin information related to the user is
 *
 * @see SessionExpirationEvent
 */
@WebListener
public class SessionExpirationListener implements HttpSessionListener {

	/** The session manager. */
	@Inject
	private SessionManager sessionManager;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionCreated(HttpSessionEvent arg0) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent arg0) {
		sessionManager.untrackUser(arg0.getSession());
	}
}