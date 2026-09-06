package com.silvionetto.finance;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TickerLookupTool {

	private final RestClient restClient;
	private final FinancialModelingPrepProperties properties;
	private final CompanyTickerCatalog companyTickerCatalog;
	private final BrapiMarketDataTool brapiMarketDataTool;
	private final AlpacaMarketDataTool alpacaMarketDataTool;
	private final PolygonTickerLookupClient polygonTickerLookupClient;

	public TickerLookupTool(
		RestClient.Builder restClientBuilder,
		FinancialModelingPrepProperties properties,
		CompanyTickerCatalog companyTickerCatalog,
		ObjectProvider<BrapiMarketDataTool> brapiMarketDataToolProvider,
		ObjectProvider<AlpacaMarketDataTool> alpacaMarketDataToolProvider,
		ObjectProvider<PolygonTickerLookupClient> polygonTickerLookupClientProvider
	) {
		this.properties = properties;
		this.companyTickerCatalog = companyTickerCatalog;
		this.brapiMarketDataTool = brapiMarketDataToolProvider.getIfAvailable();
		this.alpacaMarketDataTool = alpacaMarketDataToolProvider.getIfAvailable();
		this.polygonTickerLookupClient = polygonTickerLookupClientProvider.getIfAvailable();
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Tool(description = "Look up a stock ticker symbol for a company name using Financial Modeling Prep search")
	public String lookupTickerSymbol(String companyName) {
		if (companyName == null || companyName.isBlank()) {
			throw new IllegalArgumentException("companyName must not be blank");
		}
		List<CompanyTickerCatalog.CompanyTickerEntry> catalogMatches = this.companyTickerCatalog.load().stream()
			.filter(entry -> entry.company_name() != null && normalize(entry.company_name()).equals(normalize(companyName)))
			.sorted(Comparator.comparingInt(entry -> Math.abs(entry.company_name().length() - companyName.length())))
			.toList();

		if (!catalogMatches.isEmpty()) {
			return catalogMatches.getFirst().ticker_symbol();
		}

		if (this.brapiMarketDataTool != null) {
			String brapiTicker = this.brapiMarketDataTool.findTickerSymbol(companyName);
			if (brapiTicker != null && !brapiTicker.isBlank()) {
				List<CompanyTickerCatalog.CompanyTickerEntry> current = this.companyTickerCatalog.load();
				boolean exists = current.stream().anyMatch(entry -> brapiTicker.equalsIgnoreCase(entry.ticker_symbol()));
				if (!exists) {
					this.companyTickerCatalog.upsert(new CompanyTickerCatalog.CompanyTickerEntry(companyName, brapiTicker, "BRAPI"));
				}
				return brapiTicker;
			}
		}

		if (this.polygonTickerLookupClient != null) {
			String polygonTicker = this.polygonTickerLookupClient.lookupTickerSymbol(companyName);
			if (polygonTicker != null && !polygonTicker.isBlank()) {
				List<CompanyTickerCatalog.CompanyTickerEntry> current = this.companyTickerCatalog.load();
				boolean exists = current.stream().anyMatch(entry -> polygonTicker.equalsIgnoreCase(entry.ticker_symbol()));
				if (!exists) {
					this.companyTickerCatalog.upsert(new CompanyTickerCatalog.CompanyTickerEntry(companyName, polygonTicker, "POLYGON"));
				}
				return polygonTicker;
			}
		}

		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			throw new IllegalStateException("Financial Modeling Prep API key is not configured");
		}

		List<Map<String, Object>> matches = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/stable/search-name")
				.queryParam("query", companyName)
				.queryParam("limit", 10)
				.queryParam("apikey", this.properties.apiKey())
				.build())
			.retrieve()
			.body(new org.springframework.core.ParameterizedTypeReference<>() {});

		if (matches == null || matches.isEmpty()) {
			throw new IllegalStateException("No ticker found for company name: " + companyName);
		}

		if (matches.size() > 1) {
			List<String> exchanges = matches.stream()
				.map(match -> match.get("exchangeShortName"))
				.filter(value -> value != null && !value.toString().isBlank())
				.map(Object::toString)
				.distinct()
				.toList();
			String exchangeHint = exchanges.isEmpty() ? "Please provide the exchange." : "Please provide one of these exchanges: " + String.join(", ", exchanges) + ".";
			throw new IllegalStateException("Ambiguous company name: " + companyName + ". " + exchangeHint);
		}

		Object symbol = matches.getFirst().get("symbol");
		if (symbol == null || symbol.toString().isBlank()) {
			throw new IllegalStateException("Ticker symbol missing for company name: " + companyName);
		}
		return symbol.toString();
	}

	public StockQuote fetchQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String resolvedSymbol = canonicalizeSymbol(symbol);
		if (isBrapiAvailableFor(resolvedSymbol)) {
			return this.brapiMarketDataTool.fetchQuote(resolvedSymbol);
		}
		if (this.alpacaMarketDataTool != null) {
			try {
				return this.alpacaMarketDataTool.fetchQuote(resolvedSymbol);
			} catch (IllegalStateException ex) {
				if (!isAlpacaUnavailable(ex)) {
					throw ex;
				}
			} catch (RestClientResponseException ex) {
				if (ex.getStatusCode().value() != 402 && ex.getStatusCode().value() != 404) {
					throw ex;
				}
			}
		}
		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			throw new IllegalStateException("Financial Modeling Prep API key is not configured");
		}

		List<Map<String, Object>> matches = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/stable/quote")
				.queryParam("symbol", resolvedSymbol)
				.queryParam("apikey", this.properties.apiKey())
				.build())
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});

		if (matches == null || matches.isEmpty()) {
			throw new IllegalStateException("No quote found for symbol: " + resolvedSymbol);
		}

		Map<String, Object> quote = matches.getFirst();
		return new StockQuote(
			resolvedSymbol,
			toBigDecimal(quote.get("price")),
			toBigDecimal(quote.get("change")),
			toBigDecimal(quote.get("changesPercentage"))
		);
	}

	public String canonicalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		if (this.brapiMarketDataTool == null) {
			return symbol.trim();
		}
		return this.brapiMarketDataTool.canonicalizeSymbol(symbol);
	}

	private boolean isBrapiAvailableFor(String symbol) {
		return this.brapiMarketDataTool != null
			&& this.brapiMarketDataTool.isConfigured()
			&& this.brapiMarketDataTool.supportsSymbol(symbol);
	}

	private static boolean isAlpacaUnavailable(IllegalStateException ex) {
		String message = ex.getMessage();
		return message != null && (
			message.contains("Alpaca API key id is not configured")
				|| message.contains("Alpaca API secret key is not configured")
				|| message.contains("Alpaca base URL is not configured")
				|| message.contains("No Alpaca quote found")
		);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	@Tool(description = "Get the latest quote for a stock ticker symbol using Financial Modeling Prep")
	public String getQuote(String symbol) {
		StockQuote quote = fetchQuote(symbol);
		return "symbol=%s, price=%s, change=%s, changesPercentage=%s".formatted(
			quote.symbol(),
			quote.price(),
			quote.change(),
			quote.changePercent()
		);
	}

	private static BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}
		if (value instanceof Number number) {
			return new BigDecimal(number.toString());
		}
		String text = value.toString();
		if (text.isBlank()) {
			return null;
		}
		return new BigDecimal(text);
	}
}
