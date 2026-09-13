package com.silvionetto.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PolygonMarketDataTool {

	private static final ZoneId AMSTERDAM_ZONE = ZoneId.of("Europe/Amsterdam");
	private static final int AMSTERDAM_HISTORY_LOOKBACK_DAYS = 14;

	private final RestClient restClient;
	private final PolygonProperties properties;

	public PolygonMarketDataTool(RestClient.Builder restClientBuilder, PolygonProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	public StockQuote fetchQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			throw new IllegalStateException("Polygon API key is not configured");
		}

		String normalizedSymbol = MarketSymbolSupport.normalize(symbol);
		StockQuote realtimeQuote = fetchRealtimeQuoteOrNull(normalizedSymbol);
		if (realtimeQuote != null) {
			return realtimeQuote;
		}
		if (MarketSymbolSupport.isAmsterdamSymbol(normalizedSymbol)) {
			StockQuote latestPublishedQuote = fetchLatestPublishedAmsterdamQuoteOrNull(normalizedSymbol);
			if (latestPublishedQuote != null) {
				return latestPublishedQuote;
			}
		}

		throw new IllegalStateException("No Polygon quote found for symbol: " + normalizedSymbol);
	}

	@Tool(description = "Get open and close market prices for a stock ticker using Polygon.io")
	public String getOpenAndClosePrices(String symbol, String date) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		if (date == null || date.isBlank()) {
			throw new IllegalArgumentException("date must not be blank");
		}
		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			throw new IllegalStateException("Polygon API key is not configured");
		}

		LocalDate tradingDate = LocalDate.parse(date);
		Map<String, Object> response = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v1/open-close/{symbol}/{date}")
				.queryParam("adjusted", true)
				.queryParam("apiKey", this.properties.apiKey())
				.build(symbol, tradingDate))
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});

		if (response == null || response.isEmpty()) {
			throw new IllegalStateException("No open/close data found for symbol: " + symbol + " on " + tradingDate);
		}

		Object open = response.get("open");
		Object close = response.get("close");
		Object from = response.get("from");
		return "symbol=%s, date=%s, open=%s, close=%s".formatted(symbol, from != null ? from : tradingDate, toNumber(open), toNumber(close));
	}

	@Tool(description = "Get the latest market quote for a stock ticker using Polygon.io")
	public String getLatestQuote(String symbol) {
		StockQuote quote = fetchQuote(symbol);
		return "symbol=%s, price=%s, change=%s, changesPercentage=%s".formatted(
			quote.symbol(),
			quote.price(),
			quote.change(),
			quote.changePercent()
		);
	}

	private StockQuote fetchRealtimeQuoteOrNull(String symbol) {
		Map<String, Object> lastTradeResponse = fetchLastTradeResponseOrNull(symbol);
		BigDecimal lastTradePrice = extractPrice(lastTradeResponse, "price", "p");
		if (lastTradePrice == null) {
			return null;
		}

		BigDecimal previousClose = fetchPreviousCloseOrNull(symbol);
		BigDecimal change = null;
		BigDecimal changePercent = null;
		if (previousClose != null) {
			change = lastTradePrice.subtract(previousClose);
			changePercent = previousClose.compareTo(BigDecimal.ZERO) == 0
				? null
				: change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 10, RoundingMode.HALF_UP);
		}

		return new StockQuote(
			symbol,
			lastTradePrice,
			change,
			changePercent,
			MarketSymbolSupport.defaultCurrencyCode(symbol),
			resolveAmsterdamQuoteDate(lastTradeResponse, symbol)
		);
	}

	private StockQuote fetchLatestPublishedAmsterdamQuoteOrNull(String symbol) {
		LocalDate today = LocalDate.now(AMSTERDAM_ZONE);
		LocalDate from = today.minusDays(AMSTERDAM_HISTORY_LOOKBACK_DAYS);
		Map<String, Object> response = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/aggs/ticker/{symbol}/range/1/day/{from}/{to}")
				.queryParam("adjusted", true)
				.queryParam("sort", "desc")
				.queryParam("limit", 2)
				.queryParam("apiKey", this.properties.apiKey())
				.build(symbol, from, today))
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});

		if (response == null || response.isEmpty()) {
			return null;
		}

		List<Map<String, Object>> results = listOfMaps(response.get("results"));
		if (results.isEmpty()) {
			return null;
		}

		Map<String, Object> latestBar = results.getFirst();
		BigDecimal latestClose = extractPrice(latestBar, "close", "c");
		LocalDate latestDate = QuoteDateSupport.parseDate(latestBar.get("t"), AMSTERDAM_ZONE);
		if (latestClose == null || latestDate == null) {
			return null;
		}

		BigDecimal previousClose = results.size() > 1 ? extractPrice(results.get(1), "close", "c") : null;
		BigDecimal change = null;
		BigDecimal changePercent = null;
		if (previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
			change = latestClose.subtract(previousClose);
			changePercent = change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 10, RoundingMode.HALF_UP);
		}

		return new StockQuote(symbol, latestClose, change, changePercent, MarketSymbolSupport.defaultCurrencyCode(symbol), latestDate);
	}

	private Map<String, Object> fetchLastTradeResponseOrNull(String symbol) {
		try {
			return this.restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/v2/last/trade/{symbol}")
					.queryParam("apiKey", this.properties.apiKey())
					.build(symbol))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});
		} catch (RestClientResponseException ex) {
			if (isUnavailable(ex)) {
				return null;
			}
			throw ex;
		}
	}

	private BigDecimal fetchPreviousCloseOrNull(String symbol) {
		try {
			Map<String, Object> response = this.restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/v2/aggs/ticker/{symbol}/prev")
					.queryParam("adjusted", true)
					.queryParam("apiKey", this.properties.apiKey())
					.build(symbol))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});

			return extractPrice(response, "close", "c");
		} catch (RestClientResponseException ex) {
			if (isUnavailable(ex)) {
				return null;
			}
			throw ex;
		}
	}

	private static boolean isUnavailable(RestClientResponseException ex) {
		int status = ex.getStatusCode().value();
		return status == 402 || status == 403 || status == 404;
	}

	private static LocalDate resolveAmsterdamQuoteDate(Map<String, Object> response, String symbol) {
		if (!MarketSymbolSupport.isAmsterdamSymbol(symbol)) {
			return null;
		}
		return extractLocalDate(response, "t", "timestamp");
	}

	private static LocalDate extractLocalDate(Map<String, Object> response, String... fieldNames) {
		Object value = firstValue(response, fieldNames);
		return QuoteDateSupport.parseDate(value, AMSTERDAM_ZONE);
	}

	private static BigDecimal extractPrice(Map<String, Object> response, String... fieldNames) {
		Object value = firstValue(response, fieldNames);
		return toBigDecimal(value);
	}

	private static Object firstValue(Map<String, Object> response, String... fieldNames) {
		if (response == null || response.isEmpty()) {
			return null;
		}

		for (String fieldName : fieldNames) {
			Object directValue = response.get(fieldName);
			if (directValue != null) {
				return directValue;
			}
		}

		Object results = response.get("results");
		if (results instanceof Map<?, ?> resultMap) {
			for (String fieldName : fieldNames) {
				Object nestedValue = resultMap.get(fieldName);
				if (nestedValue != null) {
					return nestedValue;
				}
			}
		}
		if (results instanceof List<?> resultList && !resultList.isEmpty()) {
			Object first = resultList.getFirst();
			if (first instanceof Map<?, ?> firstMap) {
				for (String fieldName : fieldNames) {
					Object nestedValue = firstMap.get(fieldName);
					if (nestedValue != null) {
						return nestedValue;
					}
				}
			}
		}

		Object result = response.get("result");
		if (result instanceof Map<?, ?> resultMap) {
			for (String fieldName : fieldNames) {
				Object nestedValue = resultMap.get(fieldName);
				if (nestedValue != null) {
					return nestedValue;
				}
			}
		}
		return null;
	}

	private static List<Map<String, Object>> listOfMaps(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		return list.stream()
			.filter(Map.class::isInstance)
			.map(entry -> {
				Map<String, Object> result = new java.util.LinkedHashMap<>();
				((Map<?, ?>) entry).forEach((key, nestedValue) -> {
					if (key != null) {
						result.put(key.toString(), nestedValue);
					}
				});
				return result;
			})
			.toList();
	}

	private static String toNumber(Object value) {
		if (value == null) {
			return "null";
		}
		return value instanceof BigDecimal bigDecimal ? bigDecimal.toPlainString() : value.toString();
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
