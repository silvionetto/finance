package com.silvionetto.finance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory chat memory that stores the conversation history per request session.
 * Thread-safe: all mutating and reading operations are synchronized.
 */
@Component
public class InMemoryChatMemory {

	private final Map<String, Deque<Message>> messagesBySessionId = new HashMap<>();
	private final RequestSessionContext requestSessionContext;
	private final int maxMessages;

	@Autowired
	public InMemoryChatMemory(RequestSessionContext requestSessionContext) {
		this(requestSessionContext, 100);
	}

	InMemoryChatMemory(RequestSessionContext requestSessionContext, int maxMessages) {
		this.requestSessionContext = requestSessionContext;
		this.maxMessages = maxMessages;
	}

	public synchronized void addUserMessage(String content) {
		addMessage(new UserMessage(content));
	}

	public synchronized void addAssistantMessage(String content) {
		addMessage(new AssistantMessage(content));
	}

	private void addMessage(Message message) {
		Deque<Message> messages = currentMessages();
		messages.addLast(message);
		if (messages.size() > maxMessages) {
			messages.removeFirst();
		}
	}

	public synchronized List<Message> getMessages() {
		return new ArrayList<>(currentMessages());
	}

	public synchronized void clear() {
		currentMessages().clear();
	}

	public synchronized int size() {
		return currentMessages().size();
	}

	private Deque<Message> currentMessages() {
		String principalScope = this.requestSessionContext.requireCurrentPrincipalScope();
		return this.messagesBySessionId.computeIfAbsent(principalScope, ignored -> new ArrayDeque<>());
	}
}
