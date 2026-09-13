package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PolygonMarketDataToolTests {

	@Test
	void returnsLatestQuote() {
		PolygonProperties properties = new PolygonProperties("key", "https://api.polygon.io");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v2/last/trade/PNL.AS?apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(withSuccess("""
				{"status":"OK","results":{"p":12.34,"t":"2026-09-03T16:15:30Z"}}
				""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.polygon.io/v2/aggs/ticker/PNL.AS/prev?adjusted=true&apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(withSuccess("""
				{"status":"OK","results":[{"c":12.00}]}
				""", MediaType.APPLICATION_JSON));

		PolygonMarketDataTool tool = new PolygonMarketDataTool(builder, properties);

		StockQuote quote = tool.fetchQuote("pnl.as");

		assertThat(quote.symbol()).isEqualTo("PNL.AS");
		assertThat(quote.price()).isEqualByComparingTo("12.34");
		assertThat(quote.change()).isEqualByComparingTo("0.34");
		assertThat(quote.changePercent()).isEqualByComparingTo("2.8333333333");
		assertThat(quote.currencyCode()).isEqualTo("EUR");
		assertThat(quote.quoteDate()).isEqualTo(LocalDate.of(2026, 9, 3));
		server.verify();
	}

	@Test
	void returnsLatestQuoteWhenPreviousCloseIsUnavailable() {
		PolygonProperties properties = new PolygonProperties("key", "https://api.polygon.io");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v2/last/trade/PNL.AS?apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(withSuccess("""
				{"status":"OK","results":{"p":12.34}}
				""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.polygon.io/v2/aggs/ticker/PNL.AS/prev?adjusted=true&apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

		PolygonMarketDataTool tool = new PolygonMarketDataTool(builder, properties);

		StockQuote quote = tool.fetchQuote("PNL.AS");

		assertThat(quote.symbol()).isEqualTo("PNL.AS");
		assertThat(quote.price()).isEqualByComparingTo("12.34");
		assertThat(quote.change()).isNull();
		assertThat(quote.changePercent()).isNull();
		assertThat(quote.currencyCode()).isEqualTo("EUR");
		server.verify();
	}

	@Test
	void fallsBackToLatestPublishedAmsterdamDateWhenLatestTradeIsUnavailable() {
		PolygonProperties properties = new PolygonProperties("key", "https://api.polygon.io");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v2/last/trade/PNL.AS?apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND));
		server.expect(requestTo(matchesPattern("https://api\\.polygon\\.io/v2/aggs/ticker/PNL\\.AS/range/1/day/.+\\?adjusted=true&sort=desc&limit=2&apiKey=key")))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(withSuccess("""
				{"status":"OK","results":[{"c":12.34,"t":"2026-09-12"},{"c":12.00,"t":"2026-09-11"}]}
				""", MediaType.APPLICATION_JSON));

		PolygonMarketDataTool tool = new PolygonMarketDataTool(builder, properties);

		StockQuote quote = tool.fetchQuote("PNL.AS");

		assertThat(quote.symbol()).isEqualTo("PNL.AS");
		assertThat(quote.price()).isEqualByComparingTo("12.34");
		assertThat(quote.change()).isEqualByComparingTo("0.34");
		assertThat(quote.changePercent()).isEqualByComparingTo("2.8333333333");
		assertThat(quote.quoteDate()).isEqualTo(LocalDate.of(2026, 9, 12));
		server.verify();
	}

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

		assertThat(tool.getOpenAndClosePrices("AAPL", "2026-08-14"))
			.isEqualTo("symbol=AAPL, date=2026-08-14, open=100.12, close=101.34");
		server.verify();
	}

	@Test
	void returnsNullOpenAndCloseAsNullStrings() {
		PolygonProperties properties = new PolygonProperties("key", "https://api.polygon.io");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.polygon.io/v1/open-close/AAPL/2026-08-15?adjusted=true&apiKey=key"))
			.andExpect(method(org.springframework.http.HttpMethod.GET))
			.andRespond(withSuccess("""
				{"symbol":"AAPL","from":"2026-08-15","open":null,"close":null}
				""", MediaType.APPLICATION_JSON));

		PolygonMarketDataTool tool = new PolygonMarketDataTool(builder, properties);

		assertThat(tool.getOpenAndClosePrices("AAPL", "2026-08-15"))
			.isEqualTo("symbol=AAPL, date=2026-08-15, open=null, close=null");
		server.verify();
	}

	@Test
	void failsWhenApiKeyMissing() {
		PolygonMarketDataTool tool = new PolygonMarketDataTool(RestClient.builder(), new PolygonProperties("", "https://api.polygon.io"));

		assertThatThrownBy(() -> tool.getOpenAndClosePrices("AAPL", "2026-08-14"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Polygon API key is not configured");
	}

	@Test
	void failsWhenSymbolBlank() {
		PolygonMarketDataTool tool = new PolygonMarketDataTool(RestClient.builder(), new PolygonProperties("key", "https://api.polygon.io"));

		assertThatThrownBy(() -> tool.getOpenAndClosePrices(" ", "2026-08-14"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("symbol must not be blank");
	}

	@Test
	void failsWhenDateBlank() {
		PolygonMarketDataTool tool = new PolygonMarketDataTool(RestClient.builder(), new PolygonProperties("key", "https://api.polygon.io"));

		assertThatThrownBy(() -> tool.getOpenAndClosePrices("AAPL", " "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("date must not be blank");
	}
}
