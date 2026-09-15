package com.silvionetto.finance;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWalletRepository implements WalletRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcWalletRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<WalletHolding> findAllByOwnerId(String ownerId) {
		return this.jdbcTemplate.query(
			"""
				SELECT owner_id, symbol, company_name, quantity, average_cost, currency_code, created_at, updated_at
				FROM wallet_holdings
				WHERE owner_id = ?
				ORDER BY updated_at DESC, symbol
				""",
			this::mapRow,
			ownerId
		);
	}

	@Override
	public Optional<WalletHolding> findByOwnerIdAndSymbol(String ownerId, String symbol) {
		List<WalletHolding> results = this.jdbcTemplate.query(
			"""
				SELECT owner_id, symbol, company_name, quantity, average_cost, currency_code, created_at, updated_at
				FROM wallet_holdings
				WHERE owner_id = ? AND symbol = ?
				""",
			this::mapRow,
			ownerId,
			symbol
		);
		return results.stream().findFirst();
	}

	@Override
	public WalletHolding save(
		String ownerId,
		String symbol,
		String companyName,
		BigDecimal quantity,
		BigDecimal averageCost,
		String currencyCode,
		Instant now
	) {
		this.jdbcTemplate.update(
			"""
				INSERT INTO wallet_holdings (owner_id, symbol, company_name, quantity, average_cost, currency_code, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (owner_id, symbol) DO UPDATE SET
					company_name = EXCLUDED.company_name,
					quantity = EXCLUDED.quantity,
					average_cost = EXCLUDED.average_cost,
					currency_code = EXCLUDED.currency_code,
					updated_at = EXCLUDED.updated_at
				""",
			ownerId,
			symbol,
			companyName,
			quantity,
			averageCost,
			currencyCode,
			Timestamp.from(now),
			Timestamp.from(now)
		);
		return findByOwnerIdAndSymbol(ownerId, symbol).orElseThrow();
	}

	@Override
	public boolean deleteByOwnerIdAndSymbol(String ownerId, String symbol) {
		return this.jdbcTemplate.update(
			"DELETE FROM wallet_holdings WHERE owner_id = ? AND symbol = ?",
			ownerId,
			symbol
		) > 0;
	}

	private WalletHolding mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new WalletHolding(
			rs.getString("owner_id"),
			rs.getString("symbol"),
			rs.getString("company_name"),
			rs.getBigDecimal("quantity"),
			rs.getBigDecimal("average_cost"),
			rs.getString("currency_code"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant()
		);
	}
}
