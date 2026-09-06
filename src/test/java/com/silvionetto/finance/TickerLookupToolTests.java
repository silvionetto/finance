package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TickerLookupToolTests {

	@Test
	void returnsAaplFromCatalog() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of(new CompanyTickerCatalog.CompanyTickerEntry("Apple Inc.", "AAPL", "NASDAQ")));

		PolygonTickerLookupClient polygonClient = mock(PolygonTickerLookupClient.class);
		ObjectProvider<PolygonTickerLookupClient> polygonProvider = polygonProvider(polygonClient);
		ObjectProvider<AlpacaMarketDataTool> alpacaProvider = alpacaProvider(null);
		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, brapiProvider(null), alpacaProvider, polygonProvider);

		assertThat(tool.lookupTickerSymbol("Apple Inc.")).isEqualTo("AAPL");
		verify(catalog, never()).upsert(org.mockito.ArgumentMatchers.any());
		verifyNoInteractions(polygonClient);
	}

	@SuppressWarnings("unchecked")
    @Test
	void returnsAaplFromPolygonWhenCatalogMisses() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of(), List.of());

		PolygonTickerLookupClient polygonClient = mock(PolygonTickerLookupClient.class);
		when(polygonClient.lookupTickerSymbol(eq("Apple Inc."))).thenReturn("AAPL");
		ObjectProvider<PolygonTickerLookupClient> polygonProvider = polygonProvider(polygonClient);
		ObjectProvider<AlpacaMarketDataTool> alpacaProvider = alpacaProvider(null);

		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, brapiProvider(null), alpacaProvider, polygonProvider);

		assertThat(tool.lookupTickerSymbol("Apple Inc.")).isEqualTo("AAPL");
		verify(catalog).upsert(eq(new CompanyTickerCatalog.CompanyTickerEntry("Apple Inc.", "AAPL", "POLYGON")));
	}

	@Test
	void returnsPetr4FromBrapiWhenCatalogMisses() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of(), List.of());
		BrapiMarketDataTool brapi = mock(BrapiMarketDataTool.class);
		when(brapi.findTickerSymbol("Petrobras")).thenReturn("PETR4");

		TickerLookupTool tool = new TickerLookupTool(
			RestClient.builder(),
			new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"),
			catalog,
			brapiProvider(brapi),
			alpacaProvider(null),
			polygonProvider(null)
		);

		assertThat(tool.lookupTickerSymbol("Petrobras")).isEqualTo("PETR4");
		verify(catalog).upsert(eq(new CompanyTickerCatalog.CompanyTickerEntry("Petrobras", "PETR4", "BRAPI")));
	}

	@Test
	void returnsAaplFromFmpWhenFallbackNeeded() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of());
		ObjectProvider<PolygonTickerLookupClient> polygonProvider = polygonProvider(null);

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://financialmodelingprep.com/stable/search-name?query=Apple%20Inc.&limit=10&apikey=fmp-key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				[{"symbol":"AAPL","exchangeShortName":"NASDAQ"}]
				""", MediaType.APPLICATION_JSON));

		TickerLookupTool tool = new TickerLookupTool(builder, new FinancialModelingPrepProperties("fmp-key", "https://financialmodelingprep.com"), catalog, brapiProvider(null), alpacaProvider(null), polygonProvider);

		assertThat(tool.lookupTickerSymbol("Apple Inc.")).isEqualTo("AAPL");
		server.verify();
	}

	@Test
	void failsWithHelpfulMessageWhenFmpIsAmbiguous() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of());
		ObjectProvider<PolygonTickerLookupClient> polygonProvider = polygonProvider(null);

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://financialmodelingprep.com/stable/search-name?query=Apple&limit=10&apikey=fmp-key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				[
				  {"symbol":"AAPL","exchangeShortName":"NASDAQ"},
				  {"symbol":"APC","exchangeShortName":"NYSE"}
				]
				""", MediaType.APPLICATION_JSON));

		TickerLookupTool tool = new TickerLookupTool(builder, new FinancialModelingPrepProperties("fmp-key", "https://financialmodelingprep.com"), catalog, brapiProvider(null), alpacaProvider(null), polygonProvider);

		assertThatThrownBy(() -> tool.lookupTickerSymbol("Apple"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Ambiguous company name: Apple")
			.hasMessageContaining("NASDAQ")
			.hasMessageContaining("NYSE");
		server.verify();
	}

	@Test
	void failsWhenNoSourceCanResolveAndFmpKeyMissing() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of());
		ObjectProvider<PolygonTickerLookupClient> polygonProvider = polygonProvider(null);
		ObjectProvider<AlpacaMarketDataTool> alpacaProvider = alpacaProvider(null);

		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, brapiProvider(null), alpacaProvider, polygonProvider);

		assertThatThrownBy(() -> tool.lookupTickerSymbol("Apple Inc."))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Financial Modeling Prep API key is not configured");
	}

	@Test
	void returnsQuoteFromAlpacaWhenAvailable() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of());
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://data.alpaca.markets/v2/stocks/AAPL/snapshot?feed=iex"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("APCA-API-KEY-ID", "key-id"))
			.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("APCA-API-SECRET-KEY", "secret"))
			.andRespond(withSuccess("""
				{
				  "symbol":"AAPL",
				  "latestTrade":{"symbol":"AAPL","p":327.45,"t":"2026-09-03T10:15:30Z"},
				  "prevDailyBar":{"c":324.96}
				}
				""", MediaType.APPLICATION_JSON));

		AlpacaMarketDataTool alpaca = new AlpacaMarketDataTool(builder, new AlpacaProperties("key-id", "secret", "https://data.alpaca.markets", "iex"));
		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, brapiProvider(null), alpacaProvider(alpaca), polygonProvider(null));

		StockQuote quote = tool.fetchQuote("AAPL");

		assertThat(quote.symbol()).isEqualTo("AAPL");
		assertThat(quote.price()).isEqualByComparingTo("327.45");
		assertThat(quote.change()).isEqualByComparingTo("2.49");
		assertThat(quote.changePercent()).isEqualByComparingTo("0.7662481536");
		assertThat(quote.currencyCode()).isEqualTo("USD");
		server.verify();
	}

	@Test
	void returnsQuoteFromBrapiForBrazilianSymbol() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of());
		BrapiMarketDataTool brapi = mock(BrapiMarketDataTool.class);
		when(brapi.isConfigured()).thenReturn(true);
		when(brapi.canonicalizeSymbol("VVAR3")).thenReturn("BHIA3");
		when(brapi.supportsSymbol("BHIA3")).thenReturn(true);
		when(brapi.fetchQuote("BHIA3")).thenReturn(new StockQuote("BHIA3", new java.math.BigDecimal("5.12"), new java.math.BigDecimal("0.10"), new java.math.BigDecimal("1.99")));

		TickerLookupTool tool = new TickerLookupTool(
			RestClient.builder(),
			new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"),
			catalog,
			brapiProvider(brapi),
			alpacaProvider(null),
			polygonProvider(null)
		);

		StockQuote quote = tool.fetchQuote("VVAR3");

		assertThat(quote.symbol()).isEqualTo("BHIA3");
		assertThat(quote.price()).isEqualByComparingTo("5.12");
		assertThat(quote.currencyCode()).isEqualTo("BRL");
	}

	@Test
	void fallsBackToFmpWhenBrapiIsPresentButNotConfigured() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		when(catalog.load()).thenReturn(List.of());
		BrapiMarketDataTool brapi = mock(BrapiMarketDataTool.class);
		when(brapi.isConfigured()).thenReturn(false);
		when(brapi.canonicalizeSymbol("PETR4")).thenReturn("PETR4");

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://financialmodelingprep.com/stable/quote?symbol=PETR4&apikey=fmp-key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				[{"price":41.18,"change":-0.58,"changesPercentage":-1.39}]
				""", MediaType.APPLICATION_JSON));

		TickerLookupTool tool = new TickerLookupTool(
			builder,
			new FinancialModelingPrepProperties("fmp-key", "https://financialmodelingprep.com"),
			catalog,
			brapiProvider(brapi),
			alpacaProvider(null),
			polygonProvider(null)
		);

		StockQuote quote = tool.fetchQuote("PETR4");

		assertThat(quote.symbol()).isEqualTo("PETR4");
		assertThat(quote.price()).isEqualByComparingTo("41.18");
		assertThat(quote.currencyCode()).isEqualTo("BRL");
		verify(brapi, never()).fetchQuote("PETR4");
		server.verify();
	}

	@SuppressWarnings("unchecked")
	private static ObjectProvider<BrapiMarketDataTool> brapiProvider(BrapiMarketDataTool brapiMarketDataTool) {
		ObjectProvider<BrapiMarketDataTool> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(brapiMarketDataTool);
		return provider;
	}

	@SuppressWarnings("unchecked")
	private static ObjectProvider<PolygonTickerLookupClient> polygonProvider(PolygonTickerLookupClient polygonClient) {
		ObjectProvider<PolygonTickerLookupClient> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(polygonClient);
		return provider;
	}

	@SuppressWarnings("unchecked")
	private static ObjectProvider<AlpacaMarketDataTool> alpacaProvider(AlpacaMarketDataTool alpacaMarketDataTool) {
		ObjectProvider<AlpacaMarketDataTool> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(alpacaMarketDataTool);
		return provider;
	}
}
