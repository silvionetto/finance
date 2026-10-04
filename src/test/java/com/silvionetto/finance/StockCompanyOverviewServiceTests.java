package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockCompanyOverviewServiceTests {

	@Test
	void usesCatalogNameAndMarketWhenWatchlistOnlyContainsTicker() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		CompanyProfileService profileService = mock(CompanyProfileService.class);
		when(catalog.findByTickerSymbol("AAPL")).thenReturn(Optional.of(
			new CompanyTickerCatalog.CompanyTickerEntry("Apple Inc.", "AAPL", "NASDAQ")
		));
		when(profileService.findProfile("AAPL")).thenReturn(Optional.of(new CompanyProfile(
			"Apple Inc.",
			"NASDAQ",
			"Builds consumer electronics and software.",
			"Technology",
			"Consumer Electronics",
			"US",
			"https://www.apple.com"
		)));
		StockCompanyOverviewService service = new StockCompanyOverviewService(catalog, profileService);

		StockCompanyOverview overview = service.build(new WatchlistEntry(
			"owner", "aapl", "AAPL", Instant.EPOCH, Instant.EPOCH
		));

		assertThat(overview.symbol()).isEqualTo("AAPL");
		assertThat(overview.companyName()).isEqualTo("Apple Inc.");
		assertThat(overview.market()).isEqualTo("NASDAQ");
		assertThat(overview.description()).isEqualTo("Builds consumer electronics and software.");
		assertThat(overview.sector()).isEqualTo("Technology");
		assertThat(overview.industry()).isEqualTo("Consumer Electronics");
		assertThat(overview.country()).isEqualTo("US");
	}

	@Test
	void preservesCompanyNameAndBuildsOverviewWithoutOptionalMetadata() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		CompanyProfileService profileService = mock(CompanyProfileService.class);
		when(catalog.findByTickerSymbol("UNKNOWN")).thenReturn(Optional.empty());
		when(profileService.findProfile("UNKNOWN")).thenReturn(Optional.empty());
		StockCompanyOverviewService service = new StockCompanyOverviewService(catalog, profileService);

		StockCompanyOverview overview = service.build(new WatchlistEntry(
			"owner", "unknown", "A company name", Instant.EPOCH, Instant.EPOCH
		));

		assertThat(overview.companyName()).isEqualTo("A company name");
		assertThat(overview.market()).isNull();
		assertThat(overview.description()).isNull();
	}

	@Test
	void keepsPersistedCompanyDetailsWhenLiveProfileLookupFails() {
		CompanyTickerCatalog catalog = mock(CompanyTickerCatalog.class);
		CompanyProfileService profileService = mock(CompanyProfileService.class);
		when(catalog.findByTickerSymbol("AAPL")).thenReturn(Optional.empty());
		when(profileService.findProfile("AAPL")).thenThrow(new IllegalStateException("Provider unavailable"));
		StockCompanyOverviewService service = new StockCompanyOverviewService(catalog, profileService);

		StockCompanyOverview overview = service.build(new WatchlistEntry(
			"owner", "AAPL", "Apple Inc.", "Stored company description.", Instant.EPOCH, Instant.EPOCH
		));

		assertThat(overview.companyName()).isEqualTo("Apple Inc.");
		assertThat(overview.description()).isEqualTo("Stored company description.");
	}
}
