package com.silvionetto.finance;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	private final PolygonMarketDataTool polygonMarketDataTool;
	private final BrapiMarketDataTool brapiMarketDataTool;
	private final AlpacaMarketDataTool alpacaMarketDataTool;
	private final PolygonTickerLookupClient polygonTickerLookupClient;

	public TickerLookupTool(
		RestClient.Builder restClientBuilder,
		FinancialModelingPrepProperties properties,
		CompanyTickerCatalog companyTickerCatalog,
		ObjectProvider<PolygonMarketDataTool> polygonMarketDataToolProvider,
		ObjectProvider<BrapiMarketDataTool> brapiMarketDataToolProvider,
		ObjectProvider<AlpacaMarketDataTool> alpacaMarketDataToolProvider,
		ObjectProvider<PolygonTickerLookupClient> polygonTickerLookupClientProvider
	) {
		this.properties = properties;
		this.companyTickerCatalog = companyTickerCatalog;
		this.polygonMarketDataTool = polygonMarketDataToolProvider.getIfAvailable();
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
			.filter(entry -> entry.company_name() != null)
			.sorted(Comparator
				.comparingInt((CompanyTickerCatalog.CompanyTickerEntry entry) -> scoreCompanyMatch(normalize(companyName), catalogMatch(entry)))
				.thenComparing(entry -> !isAmsterdamCatalogEntry(entry))
				.thenComparingInt(entry -> Math.abs(entry.company_name().length() - companyName.length())))
			.toList();

		if (!catalogMatches.isEmpty() && isCatalogMatch(companyName, catalogMatches.getFirst())) {
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

		String preferredTicker = resolvePreferredTicker(companyName, matches);
		if (preferredTicker != null) {
			return preferredTicker;
		}

		if (matches.size() > 1) {
			List<String> exchanges = matches.stream()
				.map(TickerLookupTool::exchangeDescription)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
			String exchangeHint = exchanges.isEmpty() ? "Please provide the exchange." : "Please provide one of these exchanges: " + String.join(", ", exchanges) + ".";
			throw new IllegalStateException("Ambiguous company name: " + companyName + ". " + exchangeHint);
		}

		String symbol = symbolOf(matches.getFirst());
		if (symbol == null) {
			throw new IllegalStateException("Ticker symbol missing for company name: " + companyName);
		}
		return symbol;
	}

	public StockQuote fetchQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String resolvedSymbol = canonicalizeSymbol(symbol);
		if (this.polygonMarketDataTool != null) {
			try {
				return this.polygonMarketDataTool.fetchQuote(resolvedSymbol);
			} catch (IllegalStateException ex) {
				if (!isPolygonUnavailable(ex)) {
					throw ex;
				}
			} catch (RestClientResponseException ex) {
				if (!isPolygonUnavailable(ex)) {
					throw ex;
				}
			}
		}
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
			toBigDecimal(quote.get("changesPercentage")),
			resolveCurrencyCode(quote, resolvedSymbol)
		);
	}

	public String canonicalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		if (this.brapiMarketDataTool == null) {
			return MarketSymbolSupport.normalize(symbol);
		}
		return MarketSymbolSupport.normalize(this.brapiMarketDataTool.canonicalizeSymbol(symbol));
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

	private static boolean isPolygonUnavailable(IllegalStateException ex) {
		String message = ex.getMessage();
		return message != null && (
			message.contains("Polygon API key is not configured")
				|| message.contains("No Polygon quote found")
				|| message.contains("No Polygon previous close found")
		);
	}

	private static boolean isPolygonUnavailable(RestClientResponseException ex) {
		int status = ex.getStatusCode().value();
		return status == 402 || status == 403 || status == 404;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	private static boolean isCatalogMatch(String companyName, CompanyTickerCatalog.CompanyTickerEntry entry) {
		int score = scoreCompanyMatch(normalize(companyName), catalogMatch(entry));
		return score <= 2;
	}

	private static boolean isAmsterdamCatalogEntry(CompanyTickerCatalog.CompanyTickerEntry entry) {
		return MarketSymbolSupport.isAmsterdamSymbol(entry.ticker_symbol())
			|| isAmsterdamField(entry.exchange());
	}

	private static Map<String, Object> catalogMatch(CompanyTickerCatalog.CompanyTickerEntry entry) {
		Map<String, Object> values = new java.util.LinkedHashMap<>();
		values.put("symbol", entry.ticker_symbol());
		values.put("name", entry.company_name());
		values.put("exchangeShortName", entry.exchange());
		return values;
	}

	private static String resolvePreferredTicker(String companyName, List<Map<String, Object>> matches) {
		String normalizedCompanyName = normalize(companyName);
		List<Map<String, Object>> rankedMatches = matches.stream()
			.sorted(Comparator
				.comparingInt((Map<String, Object> match) -> scoreCompanyMatch(normalizedCompanyName, match))
				.thenComparing(match -> !isAmsterdamMatch(match))
				.thenComparingInt(match -> Math.abs(primaryName(match).length() - normalizedCompanyName.length())))
			.toList();

		Map<String, Object> bestMatch = rankedMatches.getFirst();
		String bestSymbol = symbolOf(bestMatch);
		if (bestSymbol == null) {
			return null;
		}
		if (rankedMatches.size() == 1) {
			return bestSymbol;
		}

		Map<String, Object> secondMatch = rankedMatches.get(1);
		int bestScore = scoreCompanyMatch(normalizedCompanyName, bestMatch);
		int secondScore = scoreCompanyMatch(normalizedCompanyName, secondMatch);
		if (bestScore < secondScore) {
			return bestSymbol;
		}
		if (isAmsterdamMatch(bestMatch) && !isAmsterdamMatch(secondMatch)) {
			return bestSymbol;
		}
		return null;
	}

	private static int scoreCompanyMatch(String normalizedSearch, Map<String, Object> match) {
		String symbol = normalize(stringValue(match.get("symbol")));
		String name = primaryName(match);
		String longName = normalize(stringValue(match.get("longName")));
		if (normalizedSearch.equals(symbol) || normalizedSearch.equals(name) || normalizedSearch.equals(longName)) {
			return 0;
		}
		if (name.startsWith(normalizedSearch) || longName.startsWith(normalizedSearch)) {
			return 1;
		}
		if (name.contains(normalizedSearch) || longName.contains(normalizedSearch)) {
			return 2;
		}
		return 3;
	}

	private static String primaryName(Map<String, Object> match) {
		return normalize(firstNonBlank(stringValue(match.get("name")), stringValue(match.get("companyName")), stringValue(match.get("longName"))));
	}

	private static boolean isAmsterdamMatch(Map<String, Object> match) {
		return MarketSymbolSupport.isAmsterdamSymbol(symbolOf(match))
			|| isAmsterdamField(match.get("exchangeShortName"))
			|| isAmsterdamField(match.get("exchange"))
			|| isAmsterdamField(match.get("stockExchange"))
			|| isAmsterdamField(match.get("exchangeFullName"));
	}

	private static boolean isAmsterdamField(Object value) {
		String normalized = normalize(stringValue(value));
		return normalized.equals("ams")
			|| normalized.contains("amsterdam")
			|| normalized.contains("euronextamsterdam");
	}

	private static String exchangeDescription(Map<String, Object> match) {
		return firstNonBlank(
			stringValue(match.get("exchangeShortName")),
			stringValue(match.get("exchange")),
			stringValue(match.get("stockExchange")),
			stringValue(match.get("exchangeFullName"))
		);
	}

	private static String symbolOf(Map<String, Object> match) {
		String symbol = stringValue(match.get("symbol"));
		return symbol == null || symbol.isBlank() ? null : MarketSymbolSupport.normalize(symbol);
	}

	private static String resolveCurrencyCode(Map<String, Object> quote, String symbol) {
		String currency = firstNonBlank(
			stringValue(quote.get("currency")),
			stringValue(quote.get("reportedCurrency"))
		);
		if (currency != null) {
			return currency.trim().toUpperCase(java.util.Locale.ROOT);
		}
		return MarketSymbolSupport.defaultCurrencyCode(symbol);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	@Tool(description = "Get the latest quote for a stock ticker symbol using the configured market data providers")
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
