package com.silvionetto.finance;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PolygonTickerLookupClient {

	private final RestClient restClient;
	private final PolygonProperties properties;

	public PolygonTickerLookupClient(RestClient.Builder restClientBuilder, PolygonProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Tool(description = "Look up a stock ticker symbol for a company name using Polygon.io")
	public String lookupTickerSymbol(String companyName) {
		if (companyName == null || companyName.isBlank()) {
			throw new IllegalArgumentException("companyName must not be blank");
		}
		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			throw new IllegalStateException("Polygon API key is not configured");
		}

		Object response = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v3/reference/tickers")
				.queryParam("search", companyName)
				.queryParam("active", true)
				.queryParam("limit", 10)
				.queryParam("apiKey", this.properties.apiKey())
				.build())
			.retrieve()
			.body(Object.class);

		String ticker = extractTicker(response);
		if (ticker == null || ticker.isBlank()) {
			throw new IllegalStateException("No Polygon ticker found for company name: " + companyName);
		}
		return ticker;
	}

	private static String extractTicker(Object response) {
		if (response instanceof Map<?, ?> payload) {
			Object directTicker = payload.get("ticker");
			if (directTicker instanceof String directTickerValue && !directTickerValue.isBlank()) {
				return directTickerValue;
			}
			Object results = payload.get("results");
			if (results instanceof List<?> resultList && !resultList.isEmpty()) {
				Object first = resultList.getFirst();
				if (first instanceof Map<?, ?> firstEntry) {
					Object tickerValue = firstEntry.get("ticker");
					return tickerValue == null ? null : tickerValue.toString();
				}
			}
		}
		if (response instanceof List<?> matches && !matches.isEmpty()) {
			Object first = matches.getFirst();
			if (first instanceof Map<?, ?> firstEntry) {
				Object tickerValue = firstEntry.get("ticker");
				return tickerValue == null ? null : tickerValue.toString();
			}
		}
		return null;
	}
}
