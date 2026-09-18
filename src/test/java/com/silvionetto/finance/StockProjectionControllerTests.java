package com.silvionetto.finance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StockProjectionControllerTests {

	@Test
	void generateReturnsProjectionPreview() throws Exception {
		StockProjectionService stockProjectionService = mock(StockProjectionService.class);
		when(stockProjectionService.generateProjection("AAPL")).thenReturn(new StockProjectionPreview(
			42L,
			"alice",
			"AAPL",
			"Projection summary",
			Instant.parse("2026-09-18T00:00:00Z")
		));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockProjectionController(stockProjectionService)).build();

		mockMvc.perform(post("/api/stock-projection")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"ticker\":\"AAPL\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tickerSymbol").value("AAPL"))
			.andExpect(jsonPath("$.content").value("Projection summary"));
	}

	@Test
	void listReturnsStoredPreviews() throws Exception {
		StockProjectionService stockProjectionService = mock(StockProjectionService.class);
		when(stockProjectionService.listRecent()).thenReturn(java.util.List.of(new StockProjectionPreview(
			7L,
			"alice",
			"PETR4",
			"Projection summary",
			Instant.parse("2026-09-18T00:00:00Z")
		)));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockProjectionController(stockProjectionService)).build();

		mockMvc.perform(get("/api/stock-projection"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].tickerSymbol").value("PETR4"));
	}
}
