package com.silvionetto.finance;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyTickerCatalog {

	private final JdbcTemplate jdbcTemplate;

	public CompanyTickerCatalog(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<CompanyTickerEntry> load() {
		return this.jdbcTemplate.query(
			"SELECT company_name, ticker_symbol, exchange FROM company_ticker_catalog ORDER BY company_name, ticker_symbol",
			(rs, rowNum) -> new CompanyTickerEntry(
				rs.getString("company_name"),
				rs.getString("ticker_symbol"),
				rs.getString("exchange")
			)
		);
	}

	public void save(List<CompanyTickerEntry> entries) {
		this.jdbcTemplate.update("DELETE FROM company_ticker_catalog");
		for (CompanyTickerEntry entry : entries) {
			upsert(entry);
		}
	}

	public void upsert(CompanyTickerEntry entry) {
		this.jdbcTemplate.update(
			"""
			INSERT INTO company_ticker_catalog (company_name, ticker_symbol, exchange)
			VALUES (?, ?, ?)
			ON CONFLICT (ticker_symbol) DO UPDATE
			SET company_name = EXCLUDED.company_name,
			    exchange = EXCLUDED.exchange
			""",
			entry.company_name(),
			entry.ticker_symbol(),
			entry.exchange()
		);
	}

	public record CompanyTickerEntry(String company_name, String ticker_symbol, String exchange) {
	}
}
