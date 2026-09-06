package com.silvionetto.finance;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class RequestSessionContext {

	private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

	public <T> T withSession(String sessionId, Supplier<T> action) {
		setCurrentSessionId(sessionId);
		try {
			return action.get();
		} finally {
			clear();
		}
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

	public void clear() {
		this.currentSessionId.remove();
	}
}
