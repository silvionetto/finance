package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WatchlistServiceTests {

	private static final String USERNAME = "user";
	private static final String DESCRIPTION = "Designs and manufactures consumer electronics.";

	@Test
	void addTickerStoresResolvedCompanyNameAndDescription() {
		Fixture fixture = new Fixture();
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.tickerLookupTool.canonicalizeSymbol("AAPL")).thenReturn("AAPL");
		when(fixture.companyProfileService.findProfile("AAPL")).thenReturn(Optional.of(profile()));
		when(fixture.repository.save(eq(USERNAME), eq("AAPL"), eq("Apple Inc."), eq(DESCRIPTION), any()))
			.thenReturn(entry("AAPL", "Apple Inc.", DESCRIPTION));

		WatchlistEntry result = fixture.service.addToWatchlist("AAPL");

		assertThat(result.companyName()).isEqualTo("Apple Inc.");
		assertThat(result.companyDescription()).isEqualTo(DESCRIPTION);
		verify(fixture.repository).save(eq(USERNAME), eq("AAPL"), eq("Apple Inc."), eq(DESCRIPTION), any());
	}

	@Test
	void addCompanyNameStoresCanonicalProviderNameAndDescription() {
		Fixture fixture = new Fixture();
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.tickerLookupTool.lookupTickerSymbol("Apple")).thenReturn("AAPL");
		when(fixture.companyProfileService.findProfile("AAPL")).thenReturn(Optional.of(profile()));
		when(fixture.repository.save(eq(USERNAME), eq("AAPL"), eq("Apple Inc."), eq(DESCRIPTION), any()))
			.thenReturn(entry("AAPL", "Apple Inc.", DESCRIPTION));

		WatchlistEntry result = fixture.service.addToWatchlist("Apple");

		assertThat(result.companyName()).isEqualTo("Apple Inc.");
		verify(fixture.tickerLookupTool).lookupTickerSymbol("Apple");
	}

	@Test
	void usesCatalogCompanyNameWhenProfileHasOnlyDescription() {
		Fixture fixture = new Fixture();
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.tickerLookupTool.canonicalizeSymbol("AAPL")).thenReturn("AAPL");
		when(fixture.companyProfileService.findProfile("AAPL")).thenReturn(Optional.of(
			new CompanyProfile(null, "NASDAQ", DESCRIPTION, "Technology", null, "US", null)
		));
		when(fixture.companyTickerCatalog.findByTickerSymbol("AAPL")).thenReturn(Optional.of(
			new CompanyTickerCatalog.CompanyTickerEntry("Apple Inc.", "AAPL", "NASDAQ")
		));
		when(fixture.repository.save(eq(USERNAME), eq("AAPL"), eq("Apple Inc."), eq(DESCRIPTION), any()))
			.thenReturn(entry("AAPL", "Apple Inc.", DESCRIPTION));

		assertThat(fixture.service.addToWatchlist("AAPL").companyName()).isEqualTo("Apple Inc.");
	}

	@Test
	void refusesToAddWhenCompanyDescriptionIsMissing() {
		Fixture fixture = new Fixture();
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.tickerLookupTool.canonicalizeSymbol("UNKNOWN")).thenReturn("UNKNOWN");
		when(fixture.companyProfileService.findProfile("UNKNOWN")).thenReturn(Optional.empty());
		when(fixture.companyTickerCatalog.findByTickerSymbol("UNKNOWN")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> fixture.service.addToWatchlist("UNKNOWN"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Company name and description could not be found");
		verify(fixture.repository, never()).save(any(), any(), any(), any(), any());
	}

	@Test
	void enrichesPreviouslySavedRowsWhenProfileIsFound() {
		Fixture fixture = new Fixture();
		WatchlistEntry legacyEntry = entry("AAPL", "AAPL", null);
		WatchlistEntry enrichedEntry = entry("AAPL", "Apple Inc.", DESCRIPTION);
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.repository.findAllByOwnerId(USERNAME)).thenReturn(List.of(legacyEntry));
		when(fixture.companyProfileService.findProfile("AAPL")).thenReturn(Optional.of(profile()));
		when(fixture.repository.updateCompanyDetails(eq(USERNAME), eq("AAPL"), eq("Apple Inc."), eq(DESCRIPTION), any()))
			.thenReturn(enrichedEntry);

		assertThat(fixture.service.listWatchlist()).containsExactly(enrichedEntry);
		verify(fixture.repository).updateCompanyDetails(eq(USERNAME), eq("AAPL"), eq("Apple Inc."), eq(DESCRIPTION), any());
	}

	@Test
	void keepsPreviouslySavedRowsWhenProfileCannotBeFound() {
		Fixture fixture = new Fixture();
		WatchlistEntry legacyEntry = entry("UNKNOWN", "UNKNOWN", null);
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.repository.findAllByOwnerId(USERNAME)).thenReturn(List.of(legacyEntry));
		when(fixture.companyProfileService.findProfile("UNKNOWN")).thenReturn(Optional.empty());
		when(fixture.companyTickerCatalog.findByTickerSymbol("UNKNOWN")).thenReturn(Optional.empty());

		assertThat(fixture.service.listWatchlist()).containsExactly(legacyEntry);
		verify(fixture.repository, never()).updateCompanyDetails(any(), any(), any(), any(), any());
	}

	@Test
	void removeFromWatchlistUsesResolvedSymbol() {
		Fixture fixture = new Fixture();
		when(fixture.authenticatedUserContext.requireCurrentUsername()).thenReturn(USERNAME);
		when(fixture.tickerLookupTool.lookupTickerSymbol("Apple")).thenReturn("AAPL");
		when(fixture.repository.deleteByOwnerIdAndSymbol(USERNAME, "AAPL")).thenReturn(true);

		assertThat(fixture.service.removeFromWatchlist("Apple")).isTrue();
		verify(fixture.repository).deleteByOwnerIdAndSymbol(USERNAME, "AAPL");
	}

	private static CompanyProfile profile() {
		return new CompanyProfile("Apple Inc.", "NASDAQ", DESCRIPTION, "Technology", "Consumer Electronics", "US", null);
	}

	private static WatchlistEntry entry(String symbol, String companyName, String description) {
		return new WatchlistEntry(USERNAME, symbol, companyName, description, Instant.EPOCH, Instant.EPOCH);
	}

	private static final class Fixture {
		private final WatchlistRepository repository = mock(WatchlistRepository.class);
		private final TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		private final CompanyProfileService companyProfileService = mock(CompanyProfileService.class);
		private final CompanyTickerCatalog companyTickerCatalog = mock(CompanyTickerCatalog.class);
		private final AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		private final WatchlistService service = new WatchlistService(
			this.repository,
			this.tickerLookupTool,
			this.companyProfileService,
			this.companyTickerCatalog,
			this.authenticatedUserContext
		);
	}
}
