package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class StockAnalysisService {
	private final ChatClient chatClient;
	private final WatchlistService watchlistService;
	private final TickerLookupTool tickerLookupTool;
	private final StockAnalysisRepository analysisRepository;
	private final MarketObservationRepository observationRepository;
	private final AuthenticatedUserContext authenticatedUserContext;

	public StockAnalysisService(ChatClient.Builder chatClientBuilder, WatchlistService watchlistService,
			TickerLookupTool tickerLookupTool, StockAnalysisRepository analysisRepository,
			MarketObservationRepository observationRepository, AuthenticatedUserContext authenticatedUserContext) {
		this.chatClient = chatClientBuilder.build();
		this.watchlistService = watchlistService;
		this.tickerLookupTool = tickerLookupTool;
		this.analysisRepository = analysisRepository;
		this.observationRepository = observationRepository;
		this.authenticatedUserContext = authenticatedUserContext;
	}

	public StockAnalysis create(String symbol) {
		String normalized = normalize(symbol);
		WatchlistEntry entry = findWatchlistEntry(normalized);
		StockQuote quote = this.tickerLookupTool.fetchQuote(normalized);
		String output = this.chatClient.prompt()
			.user("Analyze " + normalized + " for an investor. Use the supplied current market observation, explain risks and scenarios, and clearly state that this is not financial advice.\n"
				+ "Current observation: price=" + quote.price() + ", change=" + quote.change()
				+ ", changePercent=" + quote.changePercent() + ", currency=" + quote.currencyCode())
			.call().content();
		if (output == null || output.isBlank()) {
			throw new IllegalStateException("AI analysis returned no output");
		}
		return this.analysisRepository.save(this.authenticatedUserContext.requireCurrentUsername(), normalized,
			entry.companyName(), output, quote.price(), quote.change(), quote.changePercent(), quote.currencyCode(), Instant.now());
	}

	public List<StockAnalysis> list(String symbol) {
		return this.analysisRepository.findAllByOwnerIdAndSymbol(this.authenticatedUserContext.requireCurrentUsername(), normalize(symbol));
	}

	public StockComparison comparison(String symbol) {
		String normalized = normalize(symbol);
		String owner = this.authenticatedUserContext.requireCurrentUsername();
		return new StockComparison(this.analysisRepository.findAllByOwnerIdAndSymbol(owner, normalized),
			this.observationRepository.findAllByOwnerIdAndSymbol(owner, normalized));
	}

	private WatchlistEntry findWatchlistEntry(String symbol) {
		return this.watchlistService.listWatchlist().stream()
			.filter(entry -> entry.symbol().equalsIgnoreCase(symbol))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Symbol is not in the current watchlist: " + symbol));
	}

	private static String normalize(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		return symbol.trim().toUpperCase(Locale.ROOT);
	}

	public record StockComparison(List<StockAnalysis> analyses, List<MarketObservation> observations) {}
}
