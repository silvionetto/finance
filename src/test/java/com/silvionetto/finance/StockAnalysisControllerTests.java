package com.silvionetto.finance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StockAnalysisControllerTests {
	@Test
	void listsOnlyServiceReturnedAnalyses() throws Exception {
		StockAnalysisService service = mock(StockAnalysisService.class);
		when(service.list("AAPL")).thenReturn(List.of(new StockAnalysis(1L, "alice", "AAPL", "Apple",
			"Immutable report", null, null, null, "USD", Instant.parse("2026-09-18T00:00:00Z"))));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockAnalysisController(service)).build();

		mockMvc.perform(get("/api/stocks/AAPL/analyses"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].generatedOutput").value("Immutable report"))
			.andExpect(jsonPath("$[0].ownerId").value("alice"));
	}

	@Test
	void createsPrediction() throws Exception {
		StockAnalysisService service = mock(StockAnalysisService.class);
		when(service.createPrediction("AAPL")).thenReturn(new StockAnalysis(2L, "alice", "AAPL", "Apple",
			StockAnalysisType.PREDICTION, "Forecast report", null, null, null, "USD",
			Instant.parse("2026-09-18T00:00:00Z")));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockAnalysisController(service)).build();

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/stocks/AAPL/predictions"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("PREDICTION"))
			.andExpect(jsonPath("$.generatedOutput").value("Forecast report"));
	}
}
