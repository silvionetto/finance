package com.silvionetto.finance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class B3MarketDataTool {

	private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)(?:last|último|ult[ií]mo)\\\s*([0-9]+(?:[\\.,][0-9]+)?)");

	private final RestClient restClient;
	private final B3Properties properties;

	public B3MarketDataTool(RestClient.Builder restClientBuilder, B3Properties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Tool(description = "Scrape B3 market data pages for the latest price information for a single symbol")
	public String getLatestPrice(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		if (this.properties.baseUrl() == null || this.properties.baseUrl().isBlank()) {
			throw new IllegalStateException("B3 base URL is not configured");
		}

		String page = this.restClient.get()
			.uri("/pt_br/market-data/")
			.retrieve()
			.body(String.class);

		if (page == null || page.isBlank()) {
			throw new IllegalStateException("No B3 market-data page content retrieved");
		}

		String normalizedPage = page.replace('\n', ' ').replace('\r', ' ');
		Matcher matcher = PRICE_PATTERN.matcher(normalizedPage);
		if (!matcher.find()) {
			throw new IllegalStateException("Could not extract a latest price from the B3 market-data page for symbol: " + symbol);
		}

		return "symbol=%s, latestPrice=%s".formatted(symbol, matcher.group(1).replace(',', '.'));
	}
}

