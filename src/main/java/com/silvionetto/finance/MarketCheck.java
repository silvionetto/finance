package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketCheck(
	String tickerSymbol,
	String source,
	BigDecimal openPrice,
	BigDecimal closePrice,
	BigDecimal latestPrice,
	Instant checkedAt
) {
}
