package com.silvionetto.finance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWatchlistRepository implements WatchlistRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcWatchlistRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<WatchlistEntry> findAllByOwnerId(String ownerId) {
		return this.jdbcTemplate.query(
			"SELECT owner_id, symbol, company_name, created_at, updated_at FROM watchlist_entries WHERE owner_id = ? ORDER BY symbol",
			this::mapRow,
			ownerId
		);
	}

	@Override
	public Optional<WatchlistEntry> findByOwnerIdAndSymbol(String ownerId, String symbol) {
		List<WatchlistEntry> results = this.jdbcTemplate.query(
			"SELECT owner_id, symbol, company_name, created_at, updated_at FROM watchlist_entries WHERE owner_id = ? AND symbol = ?",
			this::mapRow,
			ownerId,
			symbol
		);
		return results.stream().findFirst();
	}

	@Override
	public WatchlistEntry save(String ownerId, String symbol, String companyName, Instant now) {
		this.jdbcTemplate.update(
			"INSERT INTO watchlist_entries (owner_id, symbol, company_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (owner_id, symbol) DO UPDATE SET company_name = EXCLUDED.company_name, updated_at = EXCLUDED.updated_at",
			ownerId,
			symbol,
			companyName,
			Timestamp.from(now),
			Timestamp.from(now)
		);
		return findByOwnerIdAndSymbol(ownerId, symbol).orElseThrow();
	}

	@Override
	public boolean deleteByOwnerIdAndSymbol(String ownerId, String symbol) {
		return this.jdbcTemplate.update(
			"DELETE FROM watchlist_entries WHERE owner_id = ? AND symbol = ?",
			ownerId,
			symbol
		) > 0;
	}

	private WatchlistEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new WatchlistEntry(
			rs.getString("owner_id"),
			rs.getString("symbol"),
			rs.getString("company_name"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant()
		);
	}
}
