package com.silvionetto.finance;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerTests {

	@Test
	void chatReturnsResponse() throws Exception {
		ChatService chatService = mock(ChatService.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		when(chatService.chat(anyString())).thenReturn("hello");
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService, new RequestSessionContext(), authenticatedUserContext)).build();

		mockMvc.perform(post("/api/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\":\"hi\"}"))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"response\":\"hello\"}"));
	}

	@Test
	void resetMemoryClearsConversation() throws Exception {
		ChatService chatService = mock(ChatService.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService, new RequestSessionContext(), authenticatedUserContext)).build();

		mockMvc.perform(delete("/api/chat/memory"))
			.andExpect(status().isOk());

		verify(chatService).clearMemory();
	}
}
