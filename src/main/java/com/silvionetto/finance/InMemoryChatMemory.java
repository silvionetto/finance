package com.silvionetto.finance;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple in-memory chat memory that stores the conversation history.
 * This implementation maintains all messages in a list and provides methods
 * to add and retrieve conversation context.
 */
@Component
public class InMemoryChatMemory {

	private final List<Message> messages = new ArrayList<>();
	private final int maxMessages;

	public InMemoryChatMemory() {
		this(100); // Default max 100 messages
	}

	public InMemoryChatMemory(int maxMessages) {
		this.maxMessages = maxMessages;
	}

	/**
	 * Add a user message to the conversation history.
	 */
	public void addUserMessage(String content) {
		addMessage(new UserMessage(content));
	}

	/**
	 * Add an assistant message to the conversation history.
	 */
	public void addAssistantMessage(String content) {
		addMessage(new AssistantMessage(content));
	}

	/**
	 * Add a message to the conversation history.
	 */
	private void addMessage(Message message) {
		messages.add(message);
		// Keep messages within max limit (remove oldest if exceeded)
		if (messages.size() > maxMessages) {
			messages.remove(0);
		}
	}

	/**
	 * Get all messages in the conversation history.
	 */
	public List<Message> getMessages() {
		return new ArrayList<>(messages);
	}

	/**
	 * Clear all messages from the conversation history.
	 */
	public void clear() {
		messages.clear();
	}

	/**
	 * Get the number of messages in the conversation history.
	 */
	public int size() {
		return messages.size();
	}
}

