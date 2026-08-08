package com.silvionetto.finance;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private final ChatClient chatClient;

	public ChatService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	public String chat(String prompt) {
		return this.chatClient.prompt()
			.user(prompt)
			.call()
			.content();
	}
}
