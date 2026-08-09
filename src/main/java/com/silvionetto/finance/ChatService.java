package com.silvionetto.finance;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

	private final ChatClient chatClient;
	private final DateTimeTools dateTimeTools;
	private final InMemoryChatMemory chatMemory;

	public ChatService(ChatClient.Builder chatClientBuilder, DateTimeTools dateTimeTools, InMemoryChatMemory chatMemory) {
		this.dateTimeTools = dateTimeTools;
		this.chatMemory = chatMemory;
		this.chatClient = chatClientBuilder.build();
	}

	public String chat(String prompt) {
		// Add user message to memory
		this.chatMemory.addUserMessage(prompt);

		// Build message list with conversation history
		List<Message> messages = new ArrayList<>(this.chatMemory.getMessages());

		// Call ChatClient with full conversation history
		String response = this.chatClient.prompt()
			.messages(messages)
			.tools(this.dateTimeTools)
			.call()
			.content();

		// Add assistant response to memory
		this.chatMemory.addAssistantMessage(response);

		return response;
	}

	/**
	 * Clear the conversation memory and start fresh.
	 */
	public void clearMemory() {
		this.chatMemory.clear();
	}
}
