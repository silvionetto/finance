package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PolygonTickerLookupClientTests {

	@Test
	void returnsAaplFromPolygonResultsPayload() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v3/reference/tickers?search=Apple%20Inc.&active=true&limit=10&apiKey=key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{"results":[{"ticker":"AAPL","name":"Apple Inc."}]}
				""", MediaType.APPLICATION_JSON));

		PolygonTickerLookupClient client = new PolygonTickerLookupClient(builder, new PolygonProperties("key", "https://api.polygon.io"));

		assertThat(client.lookupTickerSymbol("Apple Inc.")).isEqualTo("AAPL");
		server.verify();
	}

	@Test
	void returnsAaplFromLegacyArrayPayload() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v3/reference/tickers?search=Apple&active=true&limit=10&apiKey=key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				[{"ticker":"AAPL","name":"Apple Inc."}]
				""", MediaType.APPLICATION_JSON));

		PolygonTickerLookupClient client = new PolygonTickerLookupClient(builder, new PolygonProperties("key", "https://api.polygon.io"));

		assertThat(client.lookupTickerSymbol("Apple")).isEqualTo("AAPL");
		server.verify();
	}

	@Test
	void failsWhenApiKeyMissing() {
		PolygonTickerLookupClient client = new PolygonTickerLookupClient(RestClient.builder(), new PolygonProperties("", "https://api.polygon.io"));

		assertThatThrownBy(() -> client.lookupTickerSymbol("Apple Inc."))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Polygon API key is not configured");
	}
}
