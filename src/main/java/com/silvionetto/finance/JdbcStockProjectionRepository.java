package com.silvionetto.finance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStockProjectionRepository implements StockProjectionRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcStockProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public StockProjectionPreview save(String ownerId, String tickerSymbol, String content, Instant createdAt) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		this.jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
				"INSERT INTO stock_projection_previews (owner_id, ticker_symbol, content, created_at) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
			);
			ps.setString(1, ownerId);
			ps.setString(2, tickerSymbol);
			ps.setString(3, content);
			ps.setTimestamp(4, Timestamp.from(createdAt));
			return ps;
		}, keyHolder);

		Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
		return new StockProjectionPreview(id, ownerId, tickerSymbol, content, createdAt);
	}

	@Override
	public List<StockProjectionPreview> findAllByOwnerId(String ownerId) {
		return this.jdbcTemplate.query(
			"SELECT id, owner_id, ticker_symbol, content, created_at FROM stock_projection_previews WHERE owner_id = ? ORDER BY created_at DESC, id DESC",
			this::mapRow,
			ownerId
		);
	}

	@Override
	public Optional<StockProjectionPreview> findLatestByOwnerIdAndTicker(String ownerId, String tickerSymbol) {
		List<StockProjectionPreview> results = this.jdbcTemplate.query(
			"SELECT id, owner_id, ticker_symbol, content, created_at FROM stock_projection_previews WHERE owner_id = ? AND ticker_symbol = ? ORDER BY created_at DESC, id DESC LIMIT 1",
			this::mapRow,
			ownerId,
			tickerSymbol
		);
		return results.stream().findFirst();
	}

	private StockProjectionPreview mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new StockProjectionPreview(
			rs.getLong("id"),
			rs.getString("owner_id"),
			rs.getString("ticker_symbol"),
			rs.getString("content"),
			rs.getTimestamp("created_at").toInstant()
		);
	}
}
