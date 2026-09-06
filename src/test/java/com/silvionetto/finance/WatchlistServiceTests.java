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
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, requestSessionContext);
		when(tickerLookupTool.lookupTickerSymbol("Apple Inc")).thenReturn("AAPL");
		when(repository.save(org.mockito.ArgumentMatchers.eq("session-1"), org.mockito.ArgumentMatchers.eq("AAPL"), org.mockito.ArgumentMatchers.eq("Apple Inc"), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new WatchlistEntry("session-1", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH));

		WatchlistEntry entry = requestSessionContext.withSession("session-1", () -> service.addToWatchlist("Apple Inc"));

		assertThat(entry.symbol()).isEqualTo("AAPL");
		verify(tickerLookupTool).lookupTickerSymbol("Apple Inc");
	}

	@Test
	void listWatchlistReturnsRepositoryEntries() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, requestSessionContext);
		when(repository.findAllByOwnerId("session-1")).thenReturn(List.of(new WatchlistEntry("session-1", "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH)));

		assertThat(requestSessionContext.withSession("session-1", service::listWatchlist)).hasSize(1);
	}

	@Test
	void removeFromWatchlistUsesResolvedSymbol() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, requestSessionContext);
		when(tickerLookupTool.lookupTickerSymbol("Apple Inc")).thenReturn("AAPL");
		when(repository.deleteByOwnerIdAndSymbol("session-1", "AAPL")).thenReturn(true);

		assertThat(requestSessionContext.withSession("session-1", () -> service.removeFromWatchlist("Apple Inc"))).isTrue();
		verify(repository).deleteByOwnerIdAndSymbol("session-1", "AAPL");
	}

	@Test
	void addToWatchlistCanonicalizesBrazilianTickerSymbols() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, requestSessionContext);
		when(tickerLookupTool.canonicalizeSymbol("VVAR3")).thenReturn("BHIA3");
		when(repository.save(org.mockito.ArgumentMatchers.eq("session-1"), org.mockito.ArgumentMatchers.eq("BHIA3"), org.mockito.ArgumentMatchers.eq("VVAR3"), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new WatchlistEntry("session-1", "BHIA3", "VVAR3", Instant.EPOCH, Instant.EPOCH));

		WatchlistEntry entry = requestSessionContext.withSession("session-1", () -> service.addToWatchlist("VVAR3"));

		assertThat(entry.symbol()).isEqualTo("BHIA3");
		verify(tickerLookupTool).canonicalizeSymbol("VVAR3");
	}
}
