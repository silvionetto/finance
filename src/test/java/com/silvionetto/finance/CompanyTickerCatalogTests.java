package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CompanyTickerCatalogTests {

	@Test
	void loadsTickersFromTable() {
		JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
		CompanyTickerCatalog catalog = new CompanyTickerCatalog(jdbcTemplate);
		List<CompanyTickerCatalog.CompanyTickerEntry> expected = List.of(new CompanyTickerCatalog.CompanyTickerEntry("Apple Inc.", "AAPL", "NASDAQ"));
		org.mockito.Mockito.when(jdbcTemplate.query(
			org.mockito.ArgumentMatchers.eq("SELECT company_name, ticker_symbol, exchange FROM company_ticker_catalog ORDER BY company_name, ticker_symbol"),
			org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class)
		)).thenReturn(expected);

		List<CompanyTickerCatalog.CompanyTickerEntry> entries = catalog.load();

		assertThat(entries).isEqualTo(expected);
	}
}
