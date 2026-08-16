package com.silvionetto.finance;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TickerLookupTool {

	private final RestClient restClient;
	private final FinancialModelingPrepProperties properties;
	private final CompanyTickerCatalog companyTickerCatalog;
	private final PolygonTickerLookupClient polygonTickerLookupClient;

	public TickerLookupTool(RestClient.Builder restClientBuilder, FinancialModelingPrepProperties properties, CompanyTickerCatalog companyTickerCatalog, ObjectProvider<PolygonTickerLookupClient> polygonTickerLookupClientProvider) {
		this.properties = properties;
		this.companyTickerCatalog = companyTickerCatalog;
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

		if (this.polygonTickerLookupClient != null) {
			String polygonTicker = this.polygonTickerLookupClient.lookupTickerSymbol(companyName);
			if (polygonTicker != null && !polygonTicker.isBlank()) {
				List<CompanyTickerCatalog.CompanyTickerEntry> current = this.companyTickerCatalog.load();
				boolean exists = current.stream().anyMatch(entry -> polygonTicker.equalsIgnoreCase(entry.ticker_symbol()));
				if (!exists) {
					List<CompanyTickerCatalog.CompanyTickerEntry> refreshed = new java.util.ArrayList<>(current);
					refreshed.add(new CompanyTickerCatalog.CompanyTickerEntry(companyName, polygonTicker, "POLYGON"));
					this.companyTickerCatalog.save(refreshed);
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

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	@Tool(description = "Get the latest quote for a stock ticker symbol using Financial Modeling Prep")
	public String getQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			throw new IllegalStateException("Financial Modeling Prep API key is not configured");
		}

		List<Map<String, Object>> matches = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/stable/quote")
				.queryParam("symbol", symbol)
				.queryParam("apikey", this.properties.apiKey())
				.build())
			.retrieve()
			.body(new org.springframework.core.ParameterizedTypeReference<>() {});

		if (matches == null || matches.isEmpty()) {
			throw new IllegalStateException("No quote found for symbol: " + symbol);
		}

		Map<String, Object> quote = matches.getFirst();
		Object price = quote.get("price");
		Object change = quote.get("change");
		Object changePercent = quote.get("changesPercentage");
		return "symbol=%s, price=%s, change=%s, changesPercentage=%s".formatted(symbol, price, change, changePercent);
	}
}
