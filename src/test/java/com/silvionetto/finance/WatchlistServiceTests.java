package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WatchlistServiceTests {

	@Test
	void addToWatchlistResolvesCompanyName() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool);
		when(tickerLookupTool.lookupTickerSymbol("Apple Inc")).thenReturn("AAPL");
		when(repository.save(org.mockito.ArgumentMatchers.eq("default"), org.mockito.ArgumentMatchers.eq("AAPL"), org.mockito.ArgumentMatchers.eq("Apple Inc"), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new WatchlistEntry("default", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH));

		WatchlistEntry entry = service.addToWatchlist("Apple Inc");

		assertThat(entry.symbol()).isEqualTo("AAPL");
		verify(tickerLookupTool).lookupTickerSymbol("Apple Inc");
	}

	@Test
	void listWatchlistReturnsRepositoryEntries() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool);
		when(repository.findAllByOwnerId("default")).thenReturn(List.of(new WatchlistEntry("default", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH)));

		assertThat(service.listWatchlist()).hasSize(1);
	}

	@Test
	void removeFromWatchlistUsesResolvedSymbol() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool);
		when(tickerLookupTool.lookupTickerSymbol("Apple Inc")).thenReturn("AAPL");
		when(repository.deleteByOwnerIdAndSymbol("default", "AAPL")).thenReturn(true);

		assertThat(service.removeFromWatchlist("Apple Inc")).isTrue();
		verify(repository).deleteByOwnerIdAndSymbol("default", "AAPL");
	}
}
