package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PolygonMarketDataToolTests {

	@Test
	void returnsOpenAndClosePrices() {
		PolygonProperties properties = new PolygonProperties("key", "https://api.polygon.io");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v1/open-close/AAPL/2026-08-14?adjusted=true&apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(withSuccess("""
				{"symbol":"AAPL","from":"2026-08-14","open":100.12,"close":101.34}
				""", MediaType.APPLICATION_JSON));

		PolygonMarketDataTool tool = new PolygonMarketDataTool(builder, properties);

		org.assertj.core.api.Assertions.assertThat(tool.getOpenAndClosePrices("AAPL", "2026-08-14"))
			.isEqualTo("symbol=AAPL, date=2026-08-14, open=100.12, close=101.34");
		server.verify();
	}

	@Test
	void failsWhenApiKeyMissing() {
		PolygonMarketDataTool tool = new PolygonMarketDataTool(RestClient.builder(), new PolygonProperties("", "https://api.polygon.io"));

		assertThatThrownBy(() -> tool.getOpenAndClosePrices("AAPL", "2026-08-14"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Polygon API key is not configured");
	}
}
