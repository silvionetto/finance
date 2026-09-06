package com.silvionetto.finance;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AlpacaMarketDataTool {

	private final RestClient restClient;
	private final AlpacaProperties properties;

	public AlpacaMarketDataTool(RestClient.Builder restClientBuilder, AlpacaProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Tool(description = "Get the latest real-time quote for a stock ticker symbol using Alpaca Market Data")
	public String getLatestQuote(String symbol) {
		StockQuote quote = fetchQuote(symbol);
		return "symbol=%s, price=%s, change=%s, changesPercentage=%s".formatted(
			quote.symbol(),
			quote.price(),
			quote.change(),
			quote.changePercent()
		);
	}

	public StockQuote fetchQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		validateCredentials();

		Map<String, Object> response = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/stocks/{symbol}/snapshot")
				.queryParam("feed", defaultFeed())
				.build(symbol))
			.headers(headers -> {
				headers.set("APCA-API-KEY-ID", this.properties.apiKeyId());
				headers.set("APCA-API-SECRET-KEY", this.properties.apiSecretKey());
			})
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});

		if (response == null || response.isEmpty()) {
			throw new IllegalStateException("No Alpaca quote found for symbol: " + symbol);
		}

		return snapshotToQuote(response, symbol);
	}

	private void validateCredentials() {
		if (this.properties.apiKeyId() == null || this.properties.apiKeyId().isBlank()) {
			throw new IllegalStateException("Alpaca API key id is not configured");
		}
		if (this.properties.apiSecretKey() == null || this.properties.apiSecretKey().isBlank()) {
			throw new IllegalStateException("Alpaca API secret key is not configured");
		}
		if (this.properties.baseUrl() == null || this.properties.baseUrl().isBlank()) {
			throw new IllegalStateException("Alpaca base URL is not configured");
		}
	}

	private String defaultFeed() {
		return this.properties.feed() == null || this.properties.feed().isBlank() ? "iex" : this.properties.feed();
	}

	private static Map<String, Object> nestedMap(Object value) {
		if (!(value instanceof Map<?, ?> nested)) {
			return Map.of();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		nested.forEach((key, nestedValue) -> {
			if (key != null) {
				result.put(key.toString(), nestedValue);
			}
		});
		return result;
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static Object firstPresent(Map<String, Object> map, String... keys) {
		for (String key : keys) {
			if (map.containsKey(key) && map.get(key) != null) {
				return map.get(key);
			}
		}
		return null;
	}

	private static BigDecimal firstBigDecimal(Map<String, Object> map, String... keys) {
		Object value = firstPresent(map, keys);
		return toBigDecimal(value);
	}

	private static String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private static String toText(Object value) {
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

	private static StockQuote snapshotToQuote(Map<String, Object> response, String fallbackSymbol) {
		Map<String, Object> latestTrade = nestedMap(response.get("latestTrade"));
		Map<String, Object> latestQuote = nestedMap(response.get("latestQuote"));
		Map<String, Object> dailyBar = nestedMap(response.get("dailyBar"));
		Map<String, Object> prevDailyBar = nestedMap(response.get("prevDailyBar"));

		String symbol = firstNonBlank(
			stringValue(response.get("symbol")),
			stringValue(latestTrade.get("symbol")),
			stringValue(latestQuote.get("symbol")),
			fallbackSymbol
		);

		BigDecimal price = firstBigDecimal(latestTrade, "p", "price");
		if (price == null) {
			price = firstBigDecimal(latestQuote, "bp", "ap", "price");
		}
		if (price == null) {
			price = firstBigDecimal(dailyBar, "c", "close");
		}

		BigDecimal previousClose = firstBigDecimal(prevDailyBar, "c", "close");
		if (previousClose == null) {
			previousClose = firstBigDecimal(dailyBar, "o", "open");
		}

		BigDecimal change = null;
		BigDecimal changePercent = null;
		if (price != null && previousClose != null) {
			change = price.subtract(previousClose);
			if (previousClose.compareTo(BigDecimal.ZERO) > 0) {
				changePercent = change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 10, java.math.RoundingMode.HALF_UP);
			}
		}

		return new StockQuote(symbol, price, change, changePercent);
	}
}
