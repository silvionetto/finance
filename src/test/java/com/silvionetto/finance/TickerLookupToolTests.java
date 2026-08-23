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
		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, polygonProvider);

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

		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, polygonProvider);

		assertThat(tool.lookupTickerSymbol("Apple Inc.")).isEqualTo("AAPL");
		verify(catalog).upsert(eq(new CompanyTickerCatalog.CompanyTickerEntry("Apple Inc.", "AAPL", "POLYGON")));
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

		TickerLookupTool tool = new TickerLookupTool(builder, new FinancialModelingPrepProperties("fmp-key", "https://financialmodelingprep.com"), catalog, polygonProvider);

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

		TickerLookupTool tool = new TickerLookupTool(builder, new FinancialModelingPrepProperties("fmp-key", "https://financialmodelingprep.com"), catalog, polygonProvider);

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

		TickerLookupTool tool = new TickerLookupTool(RestClient.builder(), new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"), catalog, polygonProvider);

		assertThatThrownBy(() -> tool.lookupTickerSymbol("Apple Inc."))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Financial Modeling Prep API key is not configured");
	}

	@SuppressWarnings("unchecked")
	private static ObjectProvider<PolygonTickerLookupClient> polygonProvider(PolygonTickerLookupClient polygonClient) {
		ObjectProvider<PolygonTickerLookupClient> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(polygonClient);
		return provider;
	}
}
