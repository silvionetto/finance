package com.silvionetto.finance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatPageControllerTests {

	@Test
	void chatPageIsServed() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatPageController()).build();

		mockMvc.perform(get("/"))
			.andExpect(status().isOk());
	}
}
