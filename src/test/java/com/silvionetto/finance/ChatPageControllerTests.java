package com.silvionetto.finance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
	void loginPageIsServed() throws Exception {
		assertTrue(new ChatPageController().loginPage().equals("login"));
	}

	@Test
	void loginTemplateIncludesCsrfField() throws Exception {
		String template = StreamUtils.copyToString(
			new ClassPathResource("templates/login.html").getInputStream(),
			StandardCharsets.UTF_8
		);

		assertThat(template, containsString("xmlns:th=\"http://www.thymeleaf.org\""));
		assertThat(template, containsString("form method=\"post\" action=\"/login\""));
		assertThat(template, containsString("th:name=\"${_csrf.parameterName}\""));
		assertThat(template, containsString("th:value=\"${_csrf.token}\""));
		assertThat(template, containsString("th:if=\"${param.error}\""));
		assertThat(template, containsString("th:if=\"${param.logout}\""));
		assertThat(template, containsString("id=\"rememberMeVisual\""));
		assertThat(template, containsString("Visual preference only."));
	}

	@Test
	void errorTemplateMatchesApplicationStyling() throws Exception {
		String template = StreamUtils.copyToString(
			new ClassPathResource("templates/error.html").getInputStream(),
			StandardCharsets.UTF_8
		);

		assertThat(template, containsString("xmlns:th=\"http://www.thymeleaf.org\""));
		assertThat(template, containsString("class=\"error-shell\""));
		assertThat(template, containsString("th:text=\"${status ?: 500}\""));
		assertThat(template, containsString("th:text=\"${error ?: 'Unexpected error'}\""));
		assertThat(template, containsString("th:text=\"${message ?: 'No additional details were provided.'}\""));
		assertThat(template, containsString("th:text=\"${path ?: '/'}\""));
		assertThat(template, containsString("href=\"/login\""));
		assertThat(template, containsString("href=\"/\""));
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
		assertThat(template, containsString("class=\"card chat-card\""));
		assertThat(template, containsString("id=\"clear-chat\""));
		assertThat(template, containsString("id=\"composer-shell\""));
		assertThat(template, containsString("id=\"composer-compact-bar\""));
		assertThat(template, containsString("id=\"toggle-composer\""));
		assertThat(template, containsString("id=\"thinking-indicator\""));
		assertThat(template, containsString("class=\"thinking-indicator\""));
		assertThat(template, containsString("id=\"thinking-message\""));
		assertThat(template, containsString("id=\"thinking-elapsed\""));
		assertThat(template, containsString("class=\"thinking-progress\""));
		assertThat(template, containsString("class=\"thinking-progress-bar\""));
		assertThat(template, containsString("function scrollHistoryToLatest(force = false)"));
		assertThat(template, containsString("const thinkingMessages = ["));
		assertThat(template, containsString("function startThinkingState()"));
		assertThat(template, containsString("function stopThinkingState(options = {})"));
		assertThat(template, containsString("function updateThinkingState()"));
		assertThat(template, containsString("promptInput.addEventListener('keydown'"));
		assertThat(template, containsString("id=\"sidebar-card\""));
		assertThat(template, containsString("id=\"toggle-sidebar\""));
		assertThat(template, containsString("id=\"chat-layout\""));
		assertThat(template, containsString("id=\"open-wallet\""));
		assertThat(template, containsString("href=\"/wallet\""));
		assertThat(template, containsString(".layout[data-sidebar-collapsed=\"true\"]"));
		assertThat(template, containsString("const composerStorageKey = 'finance.composer.collapsed'"));
		assertThat(template, containsString("function setComposerCollapsed(collapsed, options = {})"));
		assertThat(template, containsString("chatCard.dataset.composerCollapsed = String(collapsed);"));
		assertThat(template, containsString("const sidebarStorageKey = 'finance.sidebar.collapsed'"));
		assertThat(template, containsString("function setSidebarCollapsed(collapsed, options = {})"));
		assertThat(template, containsString("layout.dataset.sidebarCollapsed = String(collapsed);"));
		assertThat(template, containsString("meta name=\"_csrf\""));
		assertThat(template, containsString("function jsonHeaders()"));
		assertThat(template, containsString("href=\"/account/password\""));
		assertTrue(template.indexOf("id=\"composer-main\"") < template.indexOf("id=\"thinking-indicator\""));
		assertTrue(template.indexOf("id=\"thinking-indicator\"") < template.indexOf("id=\"chat-form\""));
	}
}
