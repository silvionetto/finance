package com.silvionetto.finance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMarketObservationRepository implements MarketObservationRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcMarketObservationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public MarketObservation save(MarketObservation observation) {
		Long id = this.jdbcTemplate.queryForObject(
			"INSERT INTO market_observations (owner_id, symbol, price, change_value, change_percent, open_price, close_price, high_price, low_price, volume, currency_code, provider, observed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
			Long.class, observation.ownerId(), observation.symbol(), observation.price(), observation.change(),
			observation.changePercent(), observation.open(), observation.close(), observation.high(), observation.low(),
			observation.volume(), observation.currencyCode(), observation.provider(), Timestamp.from(observation.observedAt()));
		return new MarketObservation(id, observation.ownerId(), observation.symbol(), observation.price(), observation.change(),
			observation.changePercent(), observation.open(), observation.close(), observation.high(), observation.low(),
			observation.volume(), observation.currencyCode(), observation.provider(), observation.observedAt());
	}

	@Override
	public List<MarketObservation> findAllByOwnerIdAndSymbol(String ownerId, String symbol) {
		return this.jdbcTemplate.query(
			"SELECT id, owner_id, symbol, price, change_value, change_percent, open_price, close_price, high_price, low_price, volume, currency_code, provider, observed_at FROM market_observations WHERE owner_id = ? AND symbol = ? ORDER BY observed_at ASC, id ASC",
			this::mapRow, ownerId, symbol);
	}

	private MarketObservation mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new MarketObservation(rs.getLong("id"), rs.getString("owner_id"), rs.getString("symbol"),
			rs.getBigDecimal("price"), rs.getBigDecimal("change_value"), rs.getBigDecimal("change_percent"),
			rs.getBigDecimal("open_price"), rs.getBigDecimal("close_price"), rs.getBigDecimal("high_price"),
			rs.getBigDecimal("low_price"), rs.getBigDecimal("volume"), rs.getString("currency_code"),
			rs.getString("provider"), rs.getTimestamp("observed_at").toInstant());
	}
}
