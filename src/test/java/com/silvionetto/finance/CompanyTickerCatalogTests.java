package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CompanyTickerCatalogTests {

	@Test
	void loadsTickersFromDocsFolder() {
		TickerCatalogProperties properties = new TickerCatalogProperties("docs/tickers.json");
		CompanyTickerCatalog catalog = new CompanyTickerCatalog(properties);

		List<CompanyTickerCatalog.CompanyTickerEntry> entries = catalog.load();

		assertThat(entries).isNotEmpty();
		assertThat(entries.getFirst().company_name()).isEqualTo("Apple Inc.");
	}
}
