package com.silvionetto.finance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WalletControllerTests {

	@Test
	void listReturnsHoldings() throws Exception {
		WalletService walletService = mock(WalletService.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		when(walletService.listHoldings()).thenReturn(List.of(
			new WalletHolding("session-1", "AAPL", "Apple Inc.", new BigDecimal("4"), new BigDecimal("189.30"), "USD", Instant.EPOCH, Instant.EPOCH)
		));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WalletController(walletService, requestSessionContext)).build();

		mockMvc.perform(get("/api/wallet"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].symbol").value("AAPL"))
			.andExpect(jsonPath("$[0].quantity").value(4))
			.andExpect(jsonPath("$[0].currencyCode").value("USD"))
			.andExpect(jsonPath("$[0].ownerId").doesNotExist());
	}

	@Test
	void saveAcceptsRequestBody() throws Exception {
		WalletService walletService = mock(WalletService.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		when(walletService.saveHolding(anyString(), anyString(), any(), any(), anyString())).thenReturn(
			new WalletHolding("session-1", "AAPL", "Apple Inc.", new BigDecimal("4"), new BigDecimal("189.30"), "USD", Instant.EPOCH, Instant.EPOCH)
		);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WalletController(walletService, requestSessionContext)).build();

		mockMvc.perform(post("/api/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "companyName": "Apple Inc.",
					  "symbol": "AAPL",
					  "quantity": 4,
					  "averageCost": 189.30,
					  "currencyCode": "USD"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.symbol").value("AAPL"))
			.andExpect(jsonPath("$.companyName").value("Apple Inc."))
			.andExpect(jsonPath("$.ownerId").doesNotExist());
	}

	@Test
	void removeAcceptsRequestBody() throws Exception {
		WalletService walletService = mock(WalletService.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		when(walletService.removeHolding(anyString())).thenReturn(true);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WalletController(walletService, requestSessionContext)).build();

		mockMvc.perform(delete("/api/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"AAPL\"}"))
			.andExpect(status().isOk());
	}
}
