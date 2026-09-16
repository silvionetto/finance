package com.silvionetto.finance;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class RequestSessionContext {

	private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();
	private final ThreadLocal<String> currentPrincipalScope = new ThreadLocal<>();

	public <T> T withSession(String sessionId, String principalScope, Supplier<T> action) {
		setCurrentSessionId(sessionId);
		setCurrentPrincipalScope(principalScope);
		try {
			return action.get();
		} finally {
			clear();
		}
	}

	public <T> T withSession(String sessionId, Supplier<T> action) {
		return withSession(sessionId, sessionId, action);
	}

	public void withSession(String sessionId, String principalScope, Runnable action) {
		withSession(sessionId, principalScope, () -> {
			action.run();
			return null;
		});
	}

	public void withSession(String sessionId, Runnable action) {
		withSession(sessionId, () -> {
			action.run();
			return null;
		});
	}

	public void setCurrentSessionId(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			throw new IllegalArgumentException("sessionId must not be blank");
		}
		this.currentSessionId.set(sessionId);
	}

	public String requireCurrentSessionId() {
		String sessionId = this.currentSessionId.get();
		if (sessionId == null || sessionId.isBlank()) {
			throw new IllegalStateException("No request session is bound");
		}
		return sessionId;
	}

	public String requireCurrentPrincipalScope() {
		String principalScope = this.currentPrincipalScope.get();
		if (principalScope == null || principalScope.isBlank()) {
			throw new IllegalStateException("No request principal scope is bound");
		}
		return principalScope;
	}

	public void clear() {
		this.currentSessionId.remove();
		this.currentPrincipalScope.remove();
	}

	private void setCurrentPrincipalScope(String principalScope) {
		if (principalScope == null || principalScope.isBlank()) {
			throw new IllegalArgumentException("principalScope must not be blank");
		}
		this.currentPrincipalScope.set(principalScope);
	}
}
