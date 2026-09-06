package com.silvionetto.finance;

import java.math.BigDecimal;

public record StockQuote(
	String symbol,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent
) {
}
