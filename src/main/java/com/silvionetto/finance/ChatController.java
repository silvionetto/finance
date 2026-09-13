package com.silvionetto.finance;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;
	private final RequestSessionContext requestSessionContext;

	public ChatController(ChatService chatService, RequestSessionContext requestSessionContext) {
		this.chatService = chatService;
		this.requestSessionContext = requestSessionContext;
	}

	@PostMapping
	public ChatResponse chat(HttpSession session, @RequestBody ChatRequest request) {
		return this.requestSessionContext.withSession(session.getId(), () -> {
			ChatReply reply = this.chatService.chat(request.prompt());
			return new ChatResponse(reply.response(), reply.format().value());
		});
	}

	@DeleteMapping("/memory")
	public void resetMemory(HttpSession session) {
		this.requestSessionContext.withSession(session.getId(), this.chatService::clearMemory);
	}

	public record ChatRequest(String prompt) {}
	public record ChatResponse(String response, String format) {}
}
