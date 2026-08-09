package com.silvionetto.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryTest {

	private InMemoryChatMemory chatMemory;

	@BeforeEach
	void setUp() {
		chatMemory = new InMemoryChatMemory();
	}

	@Test
	void shouldStoreUserMessage() {
		chatMemory.addUserMessage("Hello");

		assertThat(chatMemory.size()).isEqualTo(1);
		assertThat(chatMemory.getMessages()).hasSize(1);
		assertThat(chatMemory.getMessages().get(0)).isInstanceOf(UserMessage.class);
		assertThat(chatMemory.getMessages().get(0).getText()).isEqualTo("Hello");
	}

	@Test
	void shouldStoreAssistantMessage() {
		chatMemory.addAssistantMessage("Hi there");

		assertThat(chatMemory.size()).isEqualTo(1);
		assertThat(chatMemory.getMessages()).hasSize(1);
		assertThat(chatMemory.getMessages().get(0)).isInstanceOf(AssistantMessage.class);
		assertThat(chatMemory.getMessages().get(0).getText()).isEqualTo("Hi there");
	}

	@Test
	void shouldMaintainConversationHistory() {
		chatMemory.addUserMessage("What is the capital of France?");
		chatMemory.addAssistantMessage("The capital of France is Paris.");
		chatMemory.addUserMessage("What is its population?");

		assertThat(chatMemory.size()).isEqualTo(3);
		assertThat(chatMemory.getMessages()).hasSize(3);
		assertThat(chatMemory.getMessages().get(0).getText()).isEqualTo("What is the capital of France?");
		assertThat(chatMemory.getMessages().get(1).getText()).isEqualTo("The capital of France is Paris.");
		assertThat(chatMemory.getMessages().get(2).getText()).isEqualTo("What is its population?");
	}

	@Test
	void shouldClearConversationHistory() {
		chatMemory.addUserMessage("Hello");
		chatMemory.addAssistantMessage("Hi");

		assertThat(chatMemory.size()).isEqualTo(2);

		chatMemory.clear();

		assertThat(chatMemory.size()).isEqualTo(0);
		assertThat(chatMemory.getMessages()).isEmpty();
	}

	@Test
	void shouldRespectMaxMessages() {
		InMemoryChatMemory limitedMemory = new InMemoryChatMemory(3);

		limitedMemory.addUserMessage("Message 1");
		limitedMemory.addUserMessage("Message 2");
		limitedMemory.addUserMessage("Message 3");
		limitedMemory.addUserMessage("Message 4");

		assertThat(limitedMemory.size()).isEqualTo(3);
		assertThat(limitedMemory.getMessages().get(0).getText()).isEqualTo("Message 2");
		assertThat(limitedMemory.getMessages().get(2).getText()).isEqualTo("Message 4");
	}

	@Test
	void shouldReturnIndependentCopyOfMessages() {
		chatMemory.addUserMessage("Hello");

		var messages1 = chatMemory.getMessages();
		var messages2 = chatMemory.getMessages();

		assertThat(messages1).isNotSameAs(messages2);
		assertThat(messages1).isEqualTo(messages2);
	}
}


