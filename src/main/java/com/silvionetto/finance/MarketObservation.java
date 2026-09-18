package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketObservation(
	Long id,
	String ownerId,
	String symbol,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent,
	BigDecimal open,
	BigDecimal close,
	BigDecimal high,
	BigDecimal low,
	BigDecimal volume,
	String currencyCode,
	String provider,
	Instant observedAt
) {}
