package com.silvionetto.finance;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Simple in-memory chat memory that stores the conversation history.
 * Thread-safe: all mutating and reading operations are synchronized.
 */
@Component
public class InMemoryChatMemory {

	private final Deque<Message> messages = new ArrayDeque<>();
	private final int maxMessages;

	public InMemoryChatMemory() {
		this(100);
	}

	public InMemoryChatMemory(int maxMessages) {
		this.maxMessages = maxMessages;
	}

	public synchronized void addUserMessage(String content) {
		addMessage(new UserMessage(content));
	}

	public synchronized void addAssistantMessage(String content) {
		addMessage(new AssistantMessage(content));
	}

	private void addMessage(Message message) {
		messages.addLast(message);
		if (messages.size() > maxMessages) {
			messages.removeFirst();
		}
	}

	public synchronized List<Message> getMessages() {
		return new ArrayList<>(messages);
	}

	public synchronized void clear() {
		messages.clear();
	}

	public synchronized int size() {
		return messages.size();
	}
}

