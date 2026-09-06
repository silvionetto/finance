package com.silvionetto.finance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StockCheckControllerTests {

	@Test
	void latestReturnsSnapshot() throws Exception {
		StockCheckService stockCheckService = mock(StockCheckService.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		when(stockCheckService.getLatestSnapshot()).thenReturn(new StockCheckSnapshot(
			Instant.parse("2026-09-03T00:00:00Z"),
			List.of(StockCheckItemResult.success(
				"AAPL",
				"Apple Inc.",
				new BigDecimal("200"),
				new BigDecimal("10"),
				new BigDecimal("6"),
				"USD",
				StockRecommendation.SELL,
				"Price is up 6%, which is a strong move, so trim or sell if you want to lock in gains."
			))
		));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockCheckController(stockCheckService, requestSessionContext)).build();

		mockMvc.perform(get("/api/stock-check"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.results[0].symbol").value("AAPL"))
			.andExpect(jsonPath("$.results[0].currencyCode").value("USD"))
			.andExpect(jsonPath("$.results[0].recommendation").value("SELL"));
	}

	@Test
	void refreshReturnsSnapshot() throws Exception {
		StockCheckService stockCheckService = mock(StockCheckService.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		when(stockCheckService.refreshLatestSnapshot()).thenReturn(new StockCheckSnapshot(Instant.now(), List.of()));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockCheckController(stockCheckService, requestSessionContext)).build();

		mockMvc.perform(post("/api/stock-check/refresh"))
			.andExpect(status().isOk());
	}
}
