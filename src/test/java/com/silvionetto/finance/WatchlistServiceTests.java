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

	private static final String USERNAME = "user";

	@Test
	void addToWatchlistResolvesCompanyName() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(tickerLookupTool.lookupTickerSymbol("Apple Inc")).thenReturn("AAPL");
		when(repository.save(org.mockito.ArgumentMatchers.eq(USERNAME), org.mockito.ArgumentMatchers.eq("AAPL"), org.mockito.ArgumentMatchers.eq("Apple Inc"), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new WatchlistEntry(USERNAME, "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH));

		WatchlistEntry entry = service.addToWatchlist("Apple Inc");

		assertThat(entry.symbol()).isEqualTo("AAPL");
		verify(tickerLookupTool).lookupTickerSymbol("Apple Inc");
	}

	@Test
	void listWatchlistReturnsRepositoryEntries() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(repository.findAllByOwnerId(USERNAME)).thenReturn(List.of(new WatchlistEntry(USERNAME, "AAPL", "Apple Inc", Instant.EPOCH, Instant.EPOCH)));

		assertThat(service.listWatchlist()).hasSize(1);
	}

	@Test
	void removeFromWatchlistUsesResolvedSymbol() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(tickerLookupTool.lookupTickerSymbol("Apple Inc")).thenReturn("AAPL");
		when(repository.deleteByOwnerIdAndSymbol(USERNAME, "AAPL")).thenReturn(true);

		assertThat(service.removeFromWatchlist("Apple Inc")).isTrue();
		verify(repository).deleteByOwnerIdAndSymbol(USERNAME, "AAPL");
	}

	@Test
	void addToWatchlistCanonicalizesBrazilianTickerSymbols() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(tickerLookupTool.canonicalizeSymbol("VVAR3")).thenReturn("BHIA3");
		when(repository.save(org.mockito.ArgumentMatchers.eq(USERNAME), org.mockito.ArgumentMatchers.eq("BHIA3"), org.mockito.ArgumentMatchers.eq("VVAR3"), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new WatchlistEntry(USERNAME, "BHIA3", "VVAR3", Instant.EPOCH, Instant.EPOCH));

		WatchlistEntry entry = service.addToWatchlist("VVAR3");

		assertThat(entry.symbol()).isEqualTo("BHIA3");
		verify(tickerLookupTool).canonicalizeSymbol("VVAR3");
	}

	@Test
	void addToWatchlistPreservesEuronextTickerSuffix() {
		WatchlistRepository repository = mock(WatchlistRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WatchlistService service = new WatchlistService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(tickerLookupTool.canonicalizeSymbol("abn.as")).thenReturn("ABN.AS");
		when(repository.save(org.mockito.ArgumentMatchers.eq(USERNAME), org.mockito.ArgumentMatchers.eq("ABN.AS"), org.mockito.ArgumentMatchers.eq("abn.as"), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new WatchlistEntry(USERNAME, "ABN.AS", "abn.as", Instant.EPOCH, Instant.EPOCH));

		WatchlistEntry entry = service.addToWatchlist("abn.as");

		assertThat(entry.symbol()).isEqualTo("ABN.AS");
		verify(tickerLookupTool).canonicalizeSymbol("abn.as");
	}
}
