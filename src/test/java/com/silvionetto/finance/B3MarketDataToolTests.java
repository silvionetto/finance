package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class B3MarketDataToolTests {

	@Test
	void extractsLatestPriceFromB3Page() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://www.b3.com.br/pt_br/market-data/"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("Último 123,45", MediaType.TEXT_PLAIN));

		B3MarketDataTool tool = new B3MarketDataTool(builder, new B3Properties("https://www.b3.com.br"));

		org.assertj.core.api.Assertions.assertThat(tool.getLatestPrice("PETR4"))
			.isEqualTo("symbol=PETR4, latestPrice=123.45");
		server.verify();
	}

	@Test
	void failsWhenPageDoesNotContainPrice() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://www.b3.com.br/pt_br/market-data/"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("No useful data", MediaType.TEXT_PLAIN));

		B3MarketDataTool tool = new B3MarketDataTool(builder, new B3Properties("https://www.b3.com.br"));

		assertThatThrownBy(() -> tool.getLatestPrice("PETR4"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Could not extract a latest price");
		server.verify();
	}
}
