package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatServiceTests {

	@Test
	void detectsHtmlResponseRequests() {
		assertThat(ChatService.detectRequestedFormat("Please format the output in HTML with a table and summary."))
			.isEqualTo(ChatResponseFormat.HTML);
	}

	@Test
	void detectsPlainTextResponseRequests() {
		assertThat(ChatService.detectRequestedFormat("Explain this in plain text only, without markdown."))
			.isEqualTo(ChatResponseFormat.TEXT);
	}

	@Test
	void defaultsToMarkdownForRegularPrompts() {
		assertThat(ChatService.detectRequestedFormat("What changed in the market today?"))
			.isEqualTo(ChatResponseFormat.MARKDOWN);
	}
}
