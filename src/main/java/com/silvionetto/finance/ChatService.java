package com.silvionetto.finance;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChatService {

	private static final Pattern HTML_FORMAT_REQUEST = Pattern.compile(
		"\\b(?:respond|reply|return|format|output|write|generate|create|render|show)\\b[^\\r\\n.]{0,80}\\bhtml\\b"
			+ "|\\bhtml\\b[^\\r\\n.]{0,80}\\b(?:response|reply|output|format|fragment)\\b",
		Pattern.CASE_INSENSITIVE
	);

	private static final Pattern TEXT_FORMAT_REQUEST = Pattern.compile(
		"\\b(?:plain\\s+text|text\\s+only|without\\s+markdown|no\\s+markdown|without\\s+html|no\\s+html)\\b",
		Pattern.CASE_INSENSITIVE
	);

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

	public ChatReply chat(String prompt) {
		ChatResponseFormat requestedFormat = detectRequestedFormat(prompt);
		this.chatMemory.addUserMessage(prompt);

		List<Message> messages = new ArrayList<>(this.chatMemory.getMessages());
		String formatInstruction = supplementalFormatInstruction(requestedFormat);
		if (formatInstruction != null) {
			messages.add(new UserMessage(formatInstruction));
		}

		String response = this.chatClient.prompt()
			.messages(messages)
			.tools(this.dateTimeTools, this.tickerLookupTool, this.brapiMarketDataTool, this.polygonMarketDataTool, this.alpacaMarketDataTool, this.watchlistTool)
			.call()
			.content();

		this.chatMemory.addAssistantMessage(response);

		return new ChatReply(response, requestedFormat);
	}

	static ChatResponseFormat detectRequestedFormat(String prompt) {
		if (prompt == null || prompt.isBlank()) {
			return ChatResponseFormat.MARKDOWN;
		}
		if (TEXT_FORMAT_REQUEST.matcher(prompt).find()) {
			return ChatResponseFormat.TEXT;
		}
		if (HTML_FORMAT_REQUEST.matcher(prompt).find()) {
			return ChatResponseFormat.HTML;
		}
		return ChatResponseFormat.MARKDOWN;
	}

	private static String supplementalFormatInstruction(ChatResponseFormat format) {
		return switch (format) {
			case HTML -> "Return only a polished HTML fragment for body content. Do not use Markdown fences. Do not include <html>, <head>, or <body> tags. Use semantic HTML such as sections, headings, paragraphs, lists, tables, and emphasis where appropriate.";
			case TEXT -> "Return plain text only. Do not use Markdown or HTML.";
			case MARKDOWN -> null;
		};
	}

	/**
	 * Clear the conversation memory and start fresh.
	 */
	public void clearMemory() {
		this.chatMemory.clear();
	}
}
