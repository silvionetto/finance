package com.silvionetto.finance;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WatchlistControllerTests {

	@Test
	void listReturnsEntries() throws Exception {
		WatchlistService watchlistService = mock(WatchlistService.class);
		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("default", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH)));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WatchlistController(watchlistService)).build();

		mockMvc.perform(get("/api/watchlist"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].symbol").value("AAPL"));
	}

	@Test
	void addAcceptsRequestBody() throws Exception {
		WatchlistService watchlistService = mock(WatchlistService.class);
		when(watchlistService.addToWatchlist(anyString())).thenReturn(new WatchlistEntry("default", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WatchlistController(watchlistService)).build();

		mockMvc.perform(post("/api/watchlist")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbolOrCompanyName\":\"AAPL\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.symbol").value("AAPL"));
	}

	@Test
	void removeAcceptsRequestBody() throws Exception {
		WatchlistService watchlistService = mock(WatchlistService.class);
		when(watchlistService.removeFromWatchlist(anyString())).thenReturn(true);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WatchlistController(watchlistService)).build();

		mockMvc.perform(delete("/api/watchlist")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbolOrCompanyName\":\"AAPL\"}"))
			.andExpect(status().isOk());
	}
}
