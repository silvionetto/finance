package com.silvionetto.finance;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
		when(chatService.chat(anyString())).thenReturn("hello");

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService)).build();

		mockMvc.perform(post("/api/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\":\"hi\"}"))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"response\":\"hello\"}"));
	}
}
