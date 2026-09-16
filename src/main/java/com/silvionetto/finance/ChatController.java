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
	private final AuthenticatedUserContext authenticatedUserContext;

	public ChatController(
		ChatService chatService,
		RequestSessionContext requestSessionContext,
		AuthenticatedUserContext authenticatedUserContext
	) {
		this.chatService = chatService;
		this.requestSessionContext = requestSessionContext;
		this.authenticatedUserContext = authenticatedUserContext;
	}

	@PostMapping
	public ChatResponse chat(HttpSession session, @RequestBody ChatRequest request) {
		return this.requestSessionContext.withSession(
			session.getId(),
			scopedSessionKey(session),
			() -> new ChatResponse(this.chatService.chat(request.prompt()))
		);
	}

	@DeleteMapping("/memory")
	public void resetMemory(HttpSession session) {
		this.requestSessionContext.withSession(session.getId(), scopedSessionKey(session), this.chatService::clearMemory);
	}

	private String scopedSessionKey(HttpSession session) {
		return this.authenticatedUserContext.requireCurrentUsername() + ":" + session.getId();
	}

	public record ChatRequest(String prompt) {}
	public record ChatResponse(String response) {}
}
