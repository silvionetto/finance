package com.silvionetto.finance;

import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Component
public class BrapiMarketDataTool {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;
	private final BrapiProperties properties;

	public BrapiMarketDataTool(RestClient.Builder restClientBuilder, BrapiProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	public boolean supportsSymbol(String symbol) {
		String normalized = normalizeBrazilianSymbol(symbol);
		return normalized.matches("^[A-Z]{4,6}\\d{1,2}$");
	}

	public String canonicalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String normalized = normalizeBrazilianSymbol(symbol);
		if (!supportsSymbol(normalized) || !isConfigured()) {
			return normalized;
		}

		Map<String, Object> response = get(uriBuilder -> uriBuilder
			.path("/api/v2/tickers/resolve")
			.queryParam("symbols", normalized)
			.build());

		List<Map<String, Object>> results = listOfMaps(response.get("results"));
		if (results.isEmpty()) {
			return normalized;
		}

		Object resolved = results.getFirst().get("symbol");
		return resolved == null || resolved.toString().isBlank() ? normalized : normalizeBrazilianSymbol(resolved.toString());
	}

	public String findTickerSymbol(String companyName) {
		if (companyName == null || companyName.isBlank()) {
			throw new IllegalArgumentException("companyName must not be blank");
		}
		if (!isConfigured()) {
			return null;
		}

		Map<String, Object> response = get(uriBuilder -> uriBuilder
			.path("/api/v2/tickers")
			.queryParam("search", companyName)
			.queryParam("limit", 10)
			.build());

		List<Map<String, Object>> results = listOfMaps(response.get("results"));
		if (results.isEmpty()) {
			return null;
		}

		String normalizedSearch = normalizeText(companyName);
		return results.stream()
			.filter(match -> !Boolean.FALSE.equals(booleanValue(match.get("isActive"))))
			.filter(match -> "B3".equalsIgnoreCase(stringValue(match.get("exchange"))))
			.min(Comparator
				.comparingInt((Map<String, Object> match) -> scoreCompanyMatch(normalizedSearch, match))
				.thenComparingInt(match -> Math.abs(primaryName(match).length() - normalizedSearch.length())))
			.map(match -> normalizeBrazilianSymbol(stringValue(match.get("symbol"))))
			.orElse(null);
	}

	public StockQuote fetchQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String normalized = normalizeBrazilianSymbol(symbol);
		if (!supportsSymbol(normalized)) {
			throw new IllegalArgumentException("BRAPI supports only Brazilian ticker symbols");
		}
		requireApiToken();

		SeriesResult seriesResult = fetchSeries("/api/v2/stocks/quote", normalized);
		return new StockQuote(
			seriesResult.symbol(),
			toBigDecimal(seriesResult.data().get("regularMarketPrice")),
			toBigDecimal(seriesResult.data().get("regularMarketChange")),
			toBigDecimal(seriesResult.data().get("regularMarketChangePercent"))
		);
	}

	@Tool(description = "Get the latest quote for a Brazilian stock ticker symbol using BRAPI")
	public String getLatestBrazilianQuote(String symbol) {
		StockQuote quote = fetchQuote(symbol);
		return "symbol=%s, price=%s, change=%s, changesPercentage=%s".formatted(
			quote.symbol(),
			toText(quote.price()),
			toText(quote.change()),
			toText(quote.changePercent())
		);
	}

	@Tool(description = "Get a historical price summary for a Brazilian stock ticker symbol using BRAPI")
	public String getBrazilianHistoricalPrices(String symbol, String range, String interval) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String normalized = normalizeBrazilianSymbol(symbol);
		if (!supportsSymbol(normalized)) {
			throw new IllegalArgumentException("BRAPI supports only Brazilian ticker symbols");
		}
		requireApiToken();

		String resolvedRange = defaultIfBlank(range, "1mo");
		String resolvedInterval = defaultIfBlank(interval, "1d");

		SeriesResult seriesResult = fetchSeries(uriBuilder -> uriBuilder
			.path("/api/v2/stocks/historical")
			.queryParam("symbols", normalized)
			.queryParam("range", resolvedRange)
			.queryParam("interval", resolvedInterval)
			.queryParam("sortOrder", "desc")
			.build());

		List<Map<String, Object>> prices = listOfMaps(seriesResult.data().get("historicalDataPrice"));
		if (prices.isEmpty()) {
			throw new IllegalStateException("No BRAPI historical data found for symbol: " + normalized);
		}

		Map<String, Object> latest = prices.getFirst();
		return "symbol=%s, range=%s, interval=%s, points=%d, latestDate=%s, latestClose=%s".formatted(
			seriesResult.symbol(),
			defaultIfBlank(stringValue(seriesResult.data().get("usedRange")), resolvedRange),
			defaultIfBlank(stringValue(seriesResult.data().get("usedInterval")), resolvedInterval),
			prices.size(),
			formatEpochDate(latest.get("date")),
			toText(latest.get("close"))
		);
	}

	@Tool(description = "Get dividend and corporate action summary for a Brazilian stock ticker symbol using BRAPI")
	public String getBrazilianDividends(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String normalized = normalizeBrazilianSymbol(symbol);
		if (!supportsSymbol(normalized)) {
			throw new IllegalArgumentException("BRAPI supports only Brazilian ticker symbols");
		}
		requireApiToken();

		SeriesResult seriesResult = fetchSeries("/api/v2/stocks/dividends", normalized);
		List<Map<String, Object>> cashDividends = listOfMaps(seriesResult.data().get("cashDividends"));
		List<Map<String, Object>> stockDividends = listOfMaps(seriesResult.data().get("stockDividends"));
		List<Map<String, Object>> subscriptions = listOfMaps(seriesResult.data().get("subscriptions"));

		Map<String, Object> latestCash = cashDividends.isEmpty() ? Map.of() : cashDividends.getFirst();
		return "symbol=%s, cashDividends=%d, stockDividends=%d, subscriptions=%d, latestCashLabel=%s, latestCashRate=%s, latestCashPaymentDate=%s".formatted(
			seriesResult.symbol(),
			cashDividends.size(),
			stockDividends.size(),
			subscriptions.size(),
			defaultIfBlank(stringValue(latestCash.get("label")), "n/a"),
			toText(latestCash.get("rate")),
			defaultIfBlank(firstNonBlank(stringValue(latestCash.get("paymentDate")), stringValue(latestCash.get("exDate"))), "n/a")
		);
	}

	@Tool(description = "Get company profile information for a Brazilian stock ticker symbol using BRAPI")
	public String getBrazilianCompanyProfile(String symbol) {
		SeriesResult seriesResult = fetchFundamentalSeries("/api/v2/stocks/profile", symbol);
		return "symbol=%s, name=%s, sector=%s, industry=%s, website=%s, country=%s".formatted(
			seriesResult.symbol(),
			defaultIfBlank(firstNonBlank(stringValue(seriesResult.data().get("name")), stringValue(seriesResult.data().get("longName"))), "n/a"),
			defaultIfBlank(stringValue(seriesResult.data().get("sector")), "n/a"),
			defaultIfBlank(stringValue(seriesResult.data().get("industry")), "n/a"),
			defaultIfBlank(stringValue(seriesResult.data().get("website")), "n/a"),
			defaultIfBlank(stringValue(seriesResult.data().get("country")), "n/a")
		);
	}

	@Tool(description = "Get valuation and trading statistics for a Brazilian stock ticker symbol using BRAPI")
	public String getBrazilianStatistics(String symbol) {
		SeriesResult seriesResult = fetchFundamentalSeries("/api/v2/stocks/statistics", symbol);
		return "symbol=%s, marketCap=%s, trailingPE=%s, priceToBook=%s, dividendYield=%s, beta=%s".formatted(
			seriesResult.symbol(),
			toText(seriesResult.data().get("marketCap")),
			toText(seriesResult.data().get("trailingPE")),
			toText(seriesResult.data().get("priceToBook")),
			toText(firstNonBlankObject(seriesResult.data().get("dividendYield"), seriesResult.data().get("yield"))),
			toText(seriesResult.data().get("beta"))
		);
	}

	@Tool(description = "Get financial fundamentals for a Brazilian stock ticker symbol using BRAPI")
	public String getBrazilianFinancialData(String symbol) {
		SeriesResult seriesResult = fetchFundamentalSeries("/api/v2/stocks/financial-data", symbol);
		return "symbol=%s, totalRevenue=%s, ebitda=%s, totalCash=%s, totalDebt=%s, currentRatio=%s, debtToEquity=%s, returnOnEquity=%s".formatted(
			seriesResult.symbol(),
			toText(seriesResult.data().get("totalRevenue")),
			toText(seriesResult.data().get("ebitda")),
			toText(seriesResult.data().get("totalCash")),
			toText(seriesResult.data().get("totalDebt")),
			toText(seriesResult.data().get("currentRatio")),
			toText(seriesResult.data().get("debtToEquity")),
			toText(seriesResult.data().get("returnOnEquity"))
		);
	}

	private SeriesResult fetchFundamentalSeries(String path, String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		String normalized = normalizeBrazilianSymbol(symbol);
		if (!supportsSymbol(normalized)) {
			throw new IllegalArgumentException("BRAPI supports only Brazilian ticker symbols");
		}
		requireApiToken();

		SeriesResult seriesResult = fetchSeries(path, normalized);
		if (seriesResult.data().isEmpty()) {
			throw new IllegalStateException("No BRAPI data found for symbol: " + normalized);
		}
		return seriesResult;
	}

	private SeriesResult fetchSeries(String path, String symbol) {
		return fetchSeries(uriBuilder -> uriBuilder
			.path(path)
			.queryParam("symbols", symbol)
			.build());
	}

	private SeriesResult fetchSeries(Function<UriBuilder, URI> uriFunction) {
		Map<String, Object> response = get(uriFunction);
		List<Map<String, Object>> results = listOfMaps(response.get("results"));
		if (results.isEmpty()) {
			throw new IllegalStateException("No BRAPI results found");
		}

		Map<String, Object> first = results.getFirst();
		String symbol = firstNonBlank(stringValue(first.get("symbol")), stringValue(first.get("requestedSymbol")));
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalStateException("BRAPI result did not include a symbol");
		}
		return new SeriesResult(normalizeBrazilianSymbol(symbol), map(first.get("data")));
	}

	private Map<String, Object> get(Function<UriBuilder, URI> uriFunction) {
		requireApiToken();
		Map<String, Object> response = this.restClient.get()
			.uri(uriFunction)
			.headers(headers -> headers.setBearerAuth(this.properties.apiToken()))
			.retrieve()
			.body(MAP_RESPONSE);

		if (response == null || response.isEmpty()) {
			throw new IllegalStateException("BRAPI returned an empty response");
		}
		return response;
	}

	public boolean isConfigured() {
		return this.properties.apiToken() != null && !this.properties.apiToken().isBlank();
	}

	private void requireApiToken() {
		if (!isConfigured()) {
			throw new IllegalStateException("BRAPI API token is not configured");
		}
	}

	private static int scoreCompanyMatch(String normalizedSearch, Map<String, Object> match) {
		String symbol = normalizeText(stringValue(match.get("symbol")));
		String name = primaryName(match);
		String longName = normalizeText(stringValue(match.get("longName")));
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
		return normalizeText(firstNonBlank(stringValue(match.get("name")), stringValue(match.get("longName"))));
	}

	private static String normalizeBrazilianSymbol(String symbol) {
		return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT).replace(".SA", "");
	}

	private static String normalizeText(String value) {
		if (value == null) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
			.replaceAll("\\p{M}+", "");
		return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static String defaultIfBlank(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static Object firstNonBlankObject(Object... values) {
		for (Object value : values) {
			if (value != null) {
				String text = value.toString();
				if (!text.isBlank()) {
					return value;
				}
			}
		}
		return null;
	}

	private static String formatEpochDate(Object value) {
		if (value instanceof Number number) {
			return Instant.ofEpochSecond(number.longValue()).atZone(ZoneOffset.UTC).toLocalDate().toString();
		}
		if (value instanceof String text && !text.isBlank()) {
			return text;
		}
		return "n/a";
	}

	private static Boolean booleanValue(Object value) {
		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}
		if (value == null) {
			return null;
		}
		return Boolean.valueOf(value.toString());
	}

	private static String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private static String toText(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal.toPlainString();
		}
		return value.toString();
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

	private static List<Map<String, Object>> listOfMaps(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		return list.stream().map(BrapiMarketDataTool::map).toList();
	}

	private static Map<String, Object> map(Object value) {
		if (!(value instanceof Map<?, ?> source)) {
			return Map.of();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		source.forEach((key, nestedValue) -> {
			if (key != null) {
				result.put(key.toString(), nestedValue);
			}
		});
		return result;
	}

	private record SeriesResult(String symbol, Map<String, Object> data) {
	}
}
