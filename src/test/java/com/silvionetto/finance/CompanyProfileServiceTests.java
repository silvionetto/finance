package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CompanyProfileServiceTests {

	@Test
	void findsProfileFromFinancialModelingPrep() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://financialmodelingprep.com/stable/profile?symbol=AAPL&apikey=test-key"))
			.andRespond(withSuccess("""
				[{
				  "companyName": "Apple Inc.",
				  "exchangeShortName": "NASDAQ",
				  "description": "Designs, manufactures, and markets smartphones and computers.",
				  "sector": "Technology",
				  "industry": "Consumer Electronics",
				  "country": "US",
				  "website": "https://www.apple.com"
				}]
				""", MediaType.APPLICATION_JSON));
		BrapiMarketDataTool brapi = mock(BrapiMarketDataTool.class);
		when(brapi.isConfigured()).thenReturn(false);
		CompanyProfileService service = new CompanyProfileService(
			builder,
			new FinancialModelingPrepProperties("test-key", "https://financialmodelingprep.com"),
			brapi
		);

		Optional<CompanyProfile> result = service.findProfile("aapl");

		assertThat(result).contains(new CompanyProfile(
			"Apple Inc.",
			"NASDAQ",
			"Designs, manufactures, and markets smartphones and computers.",
			"Technology",
			"Consumer Electronics",
			"US",
			"https://www.apple.com"
		));
		server.verify();
	}

	@Test
	void returnsEmptyWhenNoProfileProviderIsConfigured() {
		RestClient.Builder builder = RestClient.builder();
		BrapiMarketDataTool brapi = mock(BrapiMarketDataTool.class);
		when(brapi.isConfigured()).thenReturn(false);
		CompanyProfileService service = new CompanyProfileService(
			builder,
			new FinancialModelingPrepProperties("", "https://financialmodelingprep.com"),
			brapi
		);

		assertThat(service.findProfile("AAPL")).isEmpty();
	}
}
