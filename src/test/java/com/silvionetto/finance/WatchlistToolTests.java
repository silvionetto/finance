package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WatchlistToolTests {

	@Test
	void listWatchlistFormatsEntries() {
		WatchlistService watchlistService = mock(WatchlistService.class);
		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("default", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH)));
		WatchlistTool tool = new WatchlistTool(watchlistService);

		assertThat(tool.listWatchlist()).contains("AAPL");
	}
}
