package com.silvionetto.finance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

class ChatPageControllerTests {

	@Test
	void chatPageIsServed() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatPageController()).build();

		mockMvc.perform(get("/"))
			.andExpect(status().isOk());
	}

	@Test
	void chatTemplateIncludesMarkdownRenderingHooks() throws Exception {
		String template = StreamUtils.copyToString(
			new ClassPathResource("templates/chat.html").getInputStream(),
			StandardCharsets.UTF_8
		);

		assertThat(template, containsString("/webjars/dompurify/3.2.6/dist/purify.min.js"));
		assertThat(template, containsString("/webjars/marked/15.0.12/lib/marked.umd.js"));
		assertThat(template, containsString("function appendMarkdownMessage(role, markdown)"));
		assertThat(template, containsString("function appendAssistantMessage(text)"));
	}
}
