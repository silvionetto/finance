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
		assertThat(template, containsString("fragments/loading-indicator :: loadingIndicator"));
		assertThat(template, containsString("/js/loading-indicator.js"));
		assertThat(template, containsString("function scrollHistoryToLatest(force = false)"));
		assertThat(template, containsString("function startThinkingState()"));
		assertThat(template, containsString("function stopThinkingState(options = {})"));
		assertThat(template, containsString("function updateThinkingState()"));
		assertThat(template, containsString("promptInput.addEventListener('keydown'"));
		assertThat(template, containsString("id=\"sidebar-card\""));
		assertThat(template, containsString("id=\"toggle-sidebar\""));
		assertThat(template, containsString("id=\"chat-layout\""));
		assertThat(template, containsString("fragments/navigation"));
		assertThat(template, containsString(".layout[data-sidebar-collapsed=\"true\"]"));
		assertThat(template, containsString("const composerStorageKey = 'finance.composer.collapsed'"));
		assertThat(template, containsString("function setComposerCollapsed(collapsed, options = {})"));
		assertThat(template, containsString("chatCard.dataset.composerCollapsed = String(collapsed);"));
		assertThat(template, containsString("const sidebarStorageKey = 'finance.sidebar.collapsed'"));
		assertThat(template, containsString("function setSidebarCollapsed(collapsed, options = {})"));
		assertThat(template, containsString("layout.dataset.sidebarCollapsed = String(collapsed);"));
		assertThat(template, containsString("meta name=\"_csrf\""));
		assertThat(template, containsString("function jsonHeaders()"));
		assertTrue(template.indexOf("id=\"composer-main\"") < template.indexOf("fragments/loading-indicator"));
		assertTrue(template.indexOf("fragments/loading-indicator") < template.indexOf("id=\"chat-form\""));
	}

	@Test
	void sharedLoadingIndicatorIsGenericAndStockGenerationCleansUp() throws Exception {
		String indicator = StreamUtils.copyToString(
			new ClassPathResource("templates/fragments/loading-indicator.html").getInputStream(),
			StandardCharsets.UTF_8
		);
		String stock = StreamUtils.copyToString(
			new ClassPathResource("templates/stock.html").getInputStream(),
			StandardCharsets.UTF_8
		);

		assertThat(indicator, containsString("class=\"thinking-indicator\""));
		assertThat(indicator, containsString("Working"));
		assertThat(indicator, containsString("no model reasoning or tool details are shown"));
		assertThat(stock, containsString("fragments/loading-indicator :: loadingIndicator"));
		assertThat(stock, containsString("/js/loading-indicator.js"));
		assertThat(stock, containsString("generationButtons.forEach((button) => { button.disabled = true; });"));
		assertThat(stock, containsString("financeLoadingIndicator.start({ statusElement: status });"));
		assertThat(stock, containsString("financeLoadingIndicator.stop();"));
		assertThat(stock, containsString("window.clearTimeout(timeoutId);"));
		assertThat(stock, containsString("await response.json();"));
		assertTrue(stock.indexOf("await response.json();") < stock.indexOf("window.location.reload();"));
	}

	@Test
	void workspaceNavigationUsesSemanticResponsiveMenu() throws Exception {
		String template = StreamUtils.copyToString(
			new ClassPathResource("templates/fragments/navigation.html").getInputStream(),
			StandardCharsets.UTF_8
		);

		assertThat(template, containsString("th:fragment=\"workspaceNav(activePage)\""));
		assertThat(template, containsString("<nav"));
		assertThat(template, containsString("aria-label=\"Workspace navigation\""));
		assertThat(template, containsString("<details class=\"workspace-nav-mobile\">"));
		assertThat(template, containsString("action=\"/logout\""));
		assertThat(template, containsString("th:name=\"${_csrf.parameterName}\""));
		assertThat(template, containsString("aria-current"));
	}
}
