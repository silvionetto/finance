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

class EuronextPriceToolTests {

	@Test
	void returnsQuoteFromProductUrl() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://gateway.euronext.com/api/instrumentDetail?code=NL0011540547&codification=ISIN&exchCode=XAMS&sessionQuality=RT&view=FULL&authKey=auth-key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{
				  "instr": {
				    "longNm": "ABN AMRO BANK N.V.",
				    "currency": "EUR",
				    "mic": "XAMS",
				    "cdStand": "NL0011540547",
				    "currInstrSess": {
				      "lastPx": "43.08",
				      "openPx": "43.28",
				      "prevAdjClosingPrice": "43.52",
				      "tradedQty": "1448724.0",
				      "nbTrades": "4229",
				      "lastUpdate": "20260914-17:55:01",
				      "vwap": "43.0616",
				      "instrTradingStatus": "CLO"
				    },
				    "transco": [
				      {"code": "ABN", "codification": "MNE", "exchCode": "XAMS"}
				    ]
				  },
				  "exchange": [
				    {"exchCd": "XAMS", "exchlbl": "Euronext Amsterdam"}
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		EuronextPriceTool tool = new EuronextPriceTool(builder, new EuronextProperties("auth-key", "https://gateway.euronext.com"));

		assertThat(tool.getEuronextQuote("https://live.euronext.com/en/product/equities/NL0011540547-XAMS"))
			.isEqualTo("instrument=ABN AMRO BANK N.V., exchange=Euronext Amsterdam, symbol=ABN, productData=NL0011540547-XAMS, price=43.08, currency=EUR, quoteTimestamp=2026-09-14T17:55:01, open=43.28, previousClose=43.52, tradedQty=1448724.0, trades=4229, vwap=43.0616, status=CLO, summary=last price 43.08 EUR; opened at 43.28; previous close 43.52; volume 1448724.0 across 4229 trades; VWAP 43.0616; status CLO");
		server.verify();
	}

	@Test
	void returnsQuoteFromAmsterdamTickerSuffix() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://gateway.euronext.com/api/instrumentDetail?code=ABN&codification=MNE&exchCode=XAMS&sessionQuality=RT&view=FULL&authKey=auth-key"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{
				  "instr": {
				    "longNm": "ABN AMRO BANK N.V.",
				    "currency": "EUR",
				    "mic": "XAMS",
				    "cdStand": "NL0011540547",
				    "currInstrSess": {
				      "lastPx": 43.08,
				      "openPx": 43.28,
				      "prevAdjClosingPrice": 43.52,
				      "tradedQty": 1448724.0,
				      "nbTrades": 4229,
				      "lastUpdate": "20260914-17:55:01",
				      "vwap": 43.0616,
				      "instrTradingStatus": "CLO"
				    },
				    "transco": [
				      {"code": "ABN", "codification": "MNE", "exchCode": "XAMS"}
				    ]
				  },
				  "exchange": [
				    {"exchCd": "XAMS", "exchlbl": "Euronext Amsterdam"}
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		EuronextPriceTool tool = new EuronextPriceTool(builder, new EuronextProperties("auth-key", "https://gateway.euronext.com"));

		assertThat(tool.getEuronextQuote("ABN.AS"))
			.contains("symbol=ABN")
			.contains("productData=NL0011540547-XAMS")
			.contains("quoteTimestamp=2026-09-14T17:55:01");
		server.verify();
	}

	@Test
	void failsWhenExchangeSuffixUnsupported() {
		EuronextPriceTool tool = new EuronextPriceTool(RestClient.builder(), new EuronextProperties("auth-key", "https://gateway.euronext.com"));

		assertThatThrownBy(() -> tool.getEuronextQuote("ABN.IR"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Unsupported Euronext exchange suffix");
	}

	@Test
	void failsWhenAuthKeyMissing() {
		EuronextPriceTool tool = new EuronextPriceTool(RestClient.builder(), new EuronextProperties("", "https://gateway.euronext.com"));

		assertThatThrownBy(() -> tool.getEuronextQuote("ABN.AS"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Euronext auth key is not configured");
	}
}
