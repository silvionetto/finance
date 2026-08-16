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

		List<Map<String, Object>> matches = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v3/reference/tickers")
				.queryParam("search", companyName)
				.queryParam("active", true)
				.queryParam("limit", 10)
				.queryParam("apiKey", this.properties.apiKey())
				.build())
			.retrieve()
			.body(new org.springframework.core.ParameterizedTypeReference<>() {});

		if (matches == null || matches.isEmpty()) {
			throw new IllegalStateException("No Polygon ticker found for company name: " + companyName);
		}

		Object ticker = matches.getFirst().get("ticker");
		if (ticker == null || ticker.toString().isBlank()) {
			throw new IllegalStateException("Ticker symbol missing for company name: " + companyName);
		}
		return ticker.toString();
	}
}
