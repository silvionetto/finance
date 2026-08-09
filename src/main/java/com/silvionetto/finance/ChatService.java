package com.silvionetto.finance;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private final ChatClient chatClient;
	private final DateTimeTools dateTimeTools;

	public ChatService(ChatClient.Builder chatClientBuilder, DateTimeTools dateTimeTools) {
		this.dateTimeTools = dateTimeTools;
		this.chatClient = chatClientBuilder.build();
	}

	public String chat(String prompt) {
		return this.chatClient.prompt()
			.user(prompt)
			.tools(this.dateTimeTools)
			.call()
			.content();
	}
}
