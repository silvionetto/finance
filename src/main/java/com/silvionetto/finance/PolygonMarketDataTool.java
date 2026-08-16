package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PolygonMarketDataTool {

	private final RestClient restClient;
	private final PolygonProperties properties;

	public PolygonMarketDataTool(RestClient.Builder restClientBuilder, PolygonProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
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

	private static String toNumber(Object value) {
		if (value == null) {
			return "null";
		}
		return value instanceof BigDecimal bigDecimal ? bigDecimal.toPlainString() : value.toString();
	}
}
