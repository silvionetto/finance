package com.silvionetto.finance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStockAnalysisRepository implements StockAnalysisRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcStockAnalysisRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public StockAnalysis save(String ownerId, String symbol, String companyName, StockAnalysisType type, String generatedOutput,
			java.math.BigDecimal baselinePrice, java.math.BigDecimal baselineChange,
			java.math.BigDecimal baselineChangePercent, String baselineCurrencyCode, Instant createdAt) {
		Long id = this.jdbcTemplate.queryForObject(
			"INSERT INTO stock_analyses (owner_id, symbol, company_name, analysis_type, generated_output, baseline_price, baseline_change, baseline_change_percent, baseline_currency_code, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
			Long.class, ownerId, symbol, companyName, type.name(), generatedOutput, baselinePrice, baselineChange,
			baselineChangePercent, baselineCurrencyCode, Timestamp.from(createdAt));
		return findById(ownerId, id);
	}

	@Override
	public List<StockAnalysis> findAllByOwnerIdAndSymbol(String ownerId, String symbol) {
		return this.jdbcTemplate.query(
			"SELECT id, owner_id, symbol, company_name, analysis_type, generated_output, baseline_price, baseline_change, baseline_change_percent, baseline_currency_code, created_at FROM stock_analyses WHERE owner_id = ? AND symbol = ? ORDER BY created_at DESC, id DESC",
			this::mapRow, ownerId, symbol);
	}

	private StockAnalysis findById(String ownerId, Long id) {
		return this.jdbcTemplate.queryForObject(
			"SELECT id, owner_id, symbol, company_name, analysis_type, generated_output, baseline_price, baseline_change, baseline_change_percent, baseline_currency_code, created_at FROM stock_analyses WHERE owner_id = ? AND id = ?",
			this::mapRow, ownerId, id);
	}

	private StockAnalysis mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new StockAnalysis(rs.getLong("id"), rs.getString("owner_id"), rs.getString("symbol"),
			rs.getString("company_name"), StockAnalysisType.valueOf(rs.getString("analysis_type")), rs.getString("generated_output"),
			rs.getBigDecimal("baseline_price"), rs.getBigDecimal("baseline_change"),
			rs.getBigDecimal("baseline_change_percent"), rs.getString("baseline_currency_code"),
			rs.getTimestamp("created_at").toInstant());
	}
}
