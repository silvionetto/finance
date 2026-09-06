package com.silvionetto.finance;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

	private final ChatClient chatClient;
	private final DateTimeTools dateTimeTools;
	private final TickerLookupTool tickerLookupTool;
	private final BrapiMarketDataTool brapiMarketDataTool;
	private final PolygonMarketDataTool polygonMarketDataTool;
	private final AlpacaMarketDataTool alpacaMarketDataTool;
	private final WatchlistTool watchlistTool;
	private final InMemoryChatMemory chatMemory;

	public ChatService(
		ChatClient.Builder chatClientBuilder,
		DateTimeTools dateTimeTools,
		TickerLookupTool tickerLookupTool,
		BrapiMarketDataTool brapiMarketDataTool,
		PolygonMarketDataTool polygonMarketDataTool,
		AlpacaMarketDataTool alpacaMarketDataTool,
		WatchlistTool watchlistTool,
		InMemoryChatMemory chatMemory
	) {
		this.dateTimeTools = dateTimeTools;
		this.tickerLookupTool = tickerLookupTool;
		this.brapiMarketDataTool = brapiMarketDataTool;
		this.polygonMarketDataTool = polygonMarketDataTool;
		this.alpacaMarketDataTool = alpacaMarketDataTool;
		this.watchlistTool = watchlistTool;
		this.chatMemory = chatMemory;
		this.chatClient = chatClientBuilder.build();
	}

	public String chat(String prompt) {
		this.chatMemory.addUserMessage(prompt);

		List<Message> messages = new ArrayList<>(this.chatMemory.getMessages());

		String response = this.chatClient.prompt()
			.messages(messages)
			.tools(this.dateTimeTools, this.tickerLookupTool, this.brapiMarketDataTool, this.polygonMarketDataTool, this.alpacaMarketDataTool, this.watchlistTool)
			.call()
			.content();

		this.chatMemory.addAssistantMessage(response);

		return response;
	}

	/**
	 * Clear the conversation memory and start fresh.
	 */
	public void clearMemory() {
		this.chatMemory.clear();
	}
}
