package com.silvionetto.finance;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping
	public ChatResponse chat(@RequestBody ChatRequest request) {
		return new ChatResponse(this.chatService.chat(request.prompt()));
	}

	public record ChatRequest(String prompt) {}
	public record ChatResponse(String response) {}
}
