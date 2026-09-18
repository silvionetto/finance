package com.silvionetto.finance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class StockProjectionService {

	private static final String PROMPT_RESOURCE = "prompts/stock-projection.md";

	private final ChatClient chatClient;
	private final StockProjectionRepository stockProjectionRepository;
	private final AuthenticatedUserContext authenticatedUserContext;

	public StockProjectionService(
		ChatClient.Builder chatClientBuilder,
		StockProjectionRepository stockProjectionRepository,
		AuthenticatedUserContext authenticatedUserContext
	) {
		this.chatClient = chatClientBuilder.build();
		this.stockProjectionRepository = stockProjectionRepository;
		this.authenticatedUserContext = authenticatedUserContext;
	}

	public StockProjectionPreview generateProjection(String tickerSymbol) {
		String normalizedTicker = normalizeTicker(tickerSymbol);
		String prompt = promptForTicker(normalizedTicker);
		String content = this.chatClient.prompt()
			.user(prompt)
			.call()
			.content();
		return this.stockProjectionRepository.save(this.authenticatedUserContext.requireCurrentUsername(), normalizedTicker, content, Instant.now());
	}

	public List<StockProjectionPreview> listRecent() {
		return this.stockProjectionRepository.findAllByOwnerId(this.authenticatedUserContext.requireCurrentUsername());
	}

	public StockProjectionPreview latestForTicker(String tickerSymbol) {
		String normalizedTicker = normalizeTicker(tickerSymbol);
		return this.stockProjectionRepository.findLatestByOwnerIdAndTicker(this.authenticatedUserContext.requireCurrentUsername(), normalizedTicker)
			.orElseThrow(() -> new IllegalStateException("No projection preview has been generated for ticker %s".formatted(normalizedTicker)));
	}

	private String promptForTicker(String tickerSymbol) {
		String template;
		try {
			Resource resource = new ClassPathResource(PROMPT_RESOURCE);
			template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			template = defaultPromptTemplate();
		}
		return template.replace("[TICKER]", tickerSymbol).replace("[TICKER DA EMPRESA]", tickerSymbol);
	}

	private static String defaultPromptTemplate() {
		return "Atue como um analista quantitativo e trader profissional.\n\n"
			+ "Analise a ação [TICKER] e estime o comportamento do preço para os próximos 5 pregões.\n"
			+ "Forneça um resumo técnico, cenários e risco.\n";
	}

	private static String normalizeTicker(String tickerSymbol) {
		if (tickerSymbol == null || tickerSymbol.isBlank()) {
			throw new IllegalArgumentException("tickerSymbol must not be blank");
		}
		return tickerSymbol.trim().toUpperCase(Locale.ROOT);
	}
}
