package com.silvionetto.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryTest {

	private RequestSessionContext requestSessionContext;
	private InMemoryChatMemory chatMemory;

	@BeforeEach
	void setUp() {
		requestSessionContext = new RequestSessionContext();
		chatMemory = new InMemoryChatMemory(requestSessionContext);
	}

	@Test
	void shouldStoreUserMessage() {
		requestSessionContext.withSession("session-1", () -> chatMemory.addUserMessage("Hello"));

		requestSessionContext.withSession("session-1", () -> {
			assertThat(chatMemory.size()).isEqualTo(1);
			assertThat(chatMemory.getMessages()).hasSize(1);
			assertThat(chatMemory.getMessages().get(0)).isInstanceOf(UserMessage.class);
			assertThat(chatMemory.getMessages().get(0).getText()).isEqualTo("Hello");
		});
	}

	@Test
	void shouldStoreAssistantMessage() {
		requestSessionContext.withSession("session-1", () -> chatMemory.addAssistantMessage("Hi there"));

		requestSessionContext.withSession("session-1", () -> {
			assertThat(chatMemory.size()).isEqualTo(1);
			assertThat(chatMemory.getMessages()).hasSize(1);
			assertThat(chatMemory.getMessages().get(0)).isInstanceOf(AssistantMessage.class);
			assertThat(chatMemory.getMessages().get(0).getText()).isEqualTo("Hi there");
		});
	}

	@Test
	void shouldMaintainConversationHistory() {
		requestSessionContext.withSession("session-1", () -> chatMemory.addUserMessage("What is the capital of France?"));
		requestSessionContext.withSession("session-1", () -> chatMemory.addAssistantMessage("The capital of France is Paris."));
		requestSessionContext.withSession("session-1", () -> chatMemory.addUserMessage("What is its population?"));

		requestSessionContext.withSession("session-1", () -> {
			assertThat(chatMemory.size()).isEqualTo(3);
			assertThat(chatMemory.getMessages()).hasSize(3);
			assertThat(chatMemory.getMessages().get(0).getText()).isEqualTo("What is the capital of France?");
			assertThat(chatMemory.getMessages().get(1).getText()).isEqualTo("The capital of France is Paris.");
			assertThat(chatMemory.getMessages().get(2).getText()).isEqualTo("What is its population?");
		});
	}

	@Test
	void shouldClearConversationHistory() {
		requestSessionContext.withSession("session-1", () -> chatMemory.addUserMessage("Hello"));
		requestSessionContext.withSession("session-1", () -> chatMemory.addAssistantMessage("Hi"));

		requestSessionContext.withSession("session-1", () -> assertThat(chatMemory.size()).isEqualTo(2));

		requestSessionContext.withSession("session-1", chatMemory::clear);

		requestSessionContext.withSession("session-1", () -> {
			assertThat(chatMemory.size()).isEqualTo(0);
			assertThat(chatMemory.getMessages()).isEmpty();
		});
	}

	@Test
	void shouldRespectMaxMessages() {
		InMemoryChatMemory limitedMemory = new InMemoryChatMemory(requestSessionContext, 3);

		requestSessionContext.withSession("session-1", () -> limitedMemory.addUserMessage("Message 1"));
		requestSessionContext.withSession("session-1", () -> limitedMemory.addUserMessage("Message 2"));
		requestSessionContext.withSession("session-1", () -> limitedMemory.addUserMessage("Message 3"));
		requestSessionContext.withSession("session-1", () -> limitedMemory.addUserMessage("Message 4"));

		requestSessionContext.withSession("session-1", () -> {
			assertThat(limitedMemory.size()).isEqualTo(3);
			assertThat(limitedMemory.getMessages().get(0).getText()).isEqualTo("Message 2");
			assertThat(limitedMemory.getMessages().get(2).getText()).isEqualTo("Message 4");
		});
	}

	@Test
	void shouldReturnIndependentCopyOfMessages() {
		requestSessionContext.withSession("session-1", () -> chatMemory.addUserMessage("Hello"));

		var messages1 = requestSessionContext.withSession("session-1", chatMemory::getMessages);
		var messages2 = requestSessionContext.withSession("session-1", chatMemory::getMessages);

		assertThat(messages1).isNotSameAs(messages2);
		assertThat(messages1).isEqualTo(messages2);
	}

	@Test
	void shouldKeepSessionsIsolated() {
		requestSessionContext.withSession("session-1", () -> chatMemory.addUserMessage("Hello from session one"));
		requestSessionContext.withSession("session-2", () -> chatMemory.addUserMessage("Hello from session two"));

		assertThat(requestSessionContext.withSession("session-1", chatMemory::getMessages)).hasSize(1);
		assertThat(requestSessionContext.withSession("session-2", chatMemory::getMessages)).hasSize(1);
	}
}
