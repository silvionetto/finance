package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlpacaMarketDataToolTests {

	@Test
	void returnsLatestQuoteFromAlpacaResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://data.alpaca.markets/v2/stocks/AAPL/snapshot?feed=iex"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("APCA-API-KEY-ID", "key-id"))
			.andExpect(header("APCA-API-SECRET-KEY", "secret"))
			.andRespond(withSuccess("""
				{
				  "symbol":"AAPL",
				  "latestTrade":{"symbol":"AAPL","p":327.45,"t":"2026-09-03T10:15:30Z"},
				  "prevDailyBar":{"c":324.96}
				}
				""", MediaType.APPLICATION_JSON));

		AlpacaMarketDataTool tool = new AlpacaMarketDataTool(builder, new AlpacaProperties("key-id", "secret", "https://data.alpaca.markets", "iex"));

		assertThat(tool.getLatestQuote("AAPL"))
			.isEqualTo("symbol=AAPL, price=327.45, change=2.49, changesPercentage=0.7662481536");
		server.verify();
	}

	@Test
	void failsWhenApiKeyIdMissing() {
		AlpacaMarketDataTool tool = new AlpacaMarketDataTool(RestClient.builder(), new AlpacaProperties("", "secret", "https://data.alpaca.markets", "iex"));

		assertThatThrownBy(() -> tool.getLatestQuote("AAPL"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Alpaca API key id is not configured");
	}

	@Test
	void failsWhenSymbolBlank() {
		AlpacaMarketDataTool tool = new AlpacaMarketDataTool(RestClient.builder(), new AlpacaProperties("key-id", "secret", "https://data.alpaca.markets", "iex"));

		assertThatThrownBy(() -> tool.getLatestQuote(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("symbol must not be blank");
	}
}
