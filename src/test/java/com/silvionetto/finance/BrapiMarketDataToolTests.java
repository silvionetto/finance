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

class BrapiMarketDataToolTests {

	@Test
	void fetchQuoteReturnsBrapiQuote() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/stocks/quote?symbols=PETR4"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "PETR4",
				      "symbol": "PETR4",
				      "changed": false,
				      "data": {
				        "regularMarketPrice": 41.18,
				        "regularMarketChange": -0.58,
				        "regularMarketChangePercent": -1.39
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		StockQuote quote = tool.fetchQuote("PETR4");

		assertThat(quote.symbol()).isEqualTo("PETR4");
		assertThat(quote.price()).isEqualByComparingTo("41.18");
		assertThat(quote.change()).isEqualByComparingTo("-0.58");
		assertThat(quote.changePercent()).isEqualByComparingTo("-1.39");
		assertThat(quote.currencyCode()).isEqualTo("BRL");
		server.verify();
	}

	@Test
	void canonicalizeSymbolUsesResolveEndpoint() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/tickers/resolve?symbols=VVAR3"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "VVAR3",
				      "symbol": "BHIA3",
				      "changed": true,
				      "status": "renamed",
				      "effectiveDate": "2021-08-16"
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.canonicalizeSymbol("VVAR3")).isEqualTo("BHIA3");
		server.verify();
	}

	@Test
	void findTickerSymbolPrefersBestSearchMatch() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/tickers?search=Petrobras&limit=10"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "symbol": "PETR4",
				      "name": "Petrobras PN",
				      "longName": "Petroleo Brasileiro SA Petrobras",
				      "exchange": "B3",
				      "isActive": true
				    },
				    {
				      "symbol": "PRIO3",
				      "name": "PRIO",
				      "longName": "Petro Rio",
				      "exchange": "B3",
				      "isActive": true
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.findTickerSymbol("Petrobras")).isEqualTo("PETR4");
		server.verify();
	}

	@Test
	void returnsHistoricalSummary() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/stocks/historical?symbols=PETR4&range=1mo&interval=1d&sortOrder=desc"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "PETR4",
				      "symbol": "PETR4",
				      "changed": false,
				      "data": {
				        "usedRange": "1mo",
				        "usedInterval": "1d",
				        "historicalDataPrice": [
				          {"date": 1781233200, "close": 41.18},
				          {"date": 1781146800, "close": 40.90}
				        ]
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.getBrazilianHistoricalPrices("PETR4", "1mo", "1d"))
			.isEqualTo("symbol=PETR4, range=1mo, interval=1d, points=2, latestDate=2026-06-12, latestClose=41.18");
		server.verify();
	}

	@Test
	void returnsDividendSummary() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/stocks/dividends?symbols=ITSA4"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "ITSA4",
				      "symbol": "ITSA4",
				      "changed": false,
				      "data": {
				        "cashDividends": [
				          {
				            "label": "JCP",
				            "rate": 0.024242,
				            "paymentDate": "2026-10-01T03:00:00Z"
				          }
				        ],
				        "stockDividends": [],
				        "subscriptions": []
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.getBrazilianDividends("ITSA4"))
			.isEqualTo("symbol=ITSA4, cashDividends=1, stockDividends=0, subscriptions=0, latestCashLabel=JCP, latestCashRate=0.024242, latestCashPaymentDate=2026-10-01T03:00:00Z");
		server.verify();
	}

	@Test
	void returnsCompanyProfileSummary() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/stocks/profile?symbols=WEGE3"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "WEGE3",
				      "symbol": "WEGE3",
				      "changed": false,
				      "data": {
				        "name": "WEG",
				        "sector": "Industrials",
				        "industry": "Electrical Equipment",
				        "website": "https://ri.weg.net",
				        "country": "BRASIL"
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.getBrazilianCompanyProfile("WEGE3"))
			.isEqualTo("symbol=WEGE3, name=WEG, sector=Industrials, industry=Electrical Equipment, website=https://ri.weg.net, country=BRASIL");
		server.verify();
	}

	@Test
	void returnsStatisticsSummary() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/stocks/statistics?symbols=WEGE3"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "WEGE3",
				      "symbol": "WEGE3",
				      "changed": false,
				      "data": {
				        "marketCap": 178847730000,
				        "trailingPE": 28.445553,
				        "priceToBook": 10.078141,
				        "dividendYield": 0.03,
				        "beta": 0.6503127
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.getBrazilianStatistics("WEGE3"))
			.isEqualTo("symbol=WEGE3, marketCap=178847730000, trailingPE=28.445553, priceToBook=10.078141, dividendYield=0.03, beta=0.6503127");
		server.verify();
	}

	@Test
	void returnsFinancialDataSummary() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://brapi.dev/api/v2/stocks/financial-data?symbols=WEGE3"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "requestedSymbol": "WEGE3",
				      "symbol": "WEGE3",
				      "changed": false,
				      "data": {
				        "totalRevenue": 40193850000,
				        "ebitda": 8929845000,
				        "totalCash": 7385768000,
				        "totalDebt": 9730790000,
				        "currentRatio": 1.5479537,
				        "debtToEquity": 0.54833394,
				        "returnOnEquity": 0.378589
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		BrapiMarketDataTool tool = new BrapiMarketDataTool(builder, new BrapiProperties("token", "https://brapi.dev"));

		assertThat(tool.getBrazilianFinancialData("WEGE3"))
			.isEqualTo("symbol=WEGE3, totalRevenue=40193850000, ebitda=8929845000, totalCash=7385768000, totalDebt=9730790000, currentRatio=1.5479537, debtToEquity=0.54833394, returnOnEquity=0.378589");
		server.verify();
	}

	@Test
	void failsWhenApiTokenMissing() {
		BrapiMarketDataTool tool = new BrapiMarketDataTool(RestClient.builder(), new BrapiProperties("", "https://brapi.dev"));

		assertThatThrownBy(() -> tool.fetchQuote("PETR4"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("BRAPI API token is not configured");
	}
}
