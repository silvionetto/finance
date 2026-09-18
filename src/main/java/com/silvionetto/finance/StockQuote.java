package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuote(
	String symbol,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent,
	String currencyCode,
	BigDecimal open,
	BigDecimal close,
	BigDecimal high,
	BigDecimal low,
	BigDecimal volume,
	String provider,
	Instant observedAt
) {
	public StockQuote(String symbol, BigDecimal price, BigDecimal change, BigDecimal changePercent, String currencyCode) {
		this(symbol, price, change, changePercent, currencyCode, null, null, null, null, null, null, Instant.now());
	}

	public StockQuote(String symbol, BigDecimal price, BigDecimal change, BigDecimal changePercent) {
		this(symbol, price, change, changePercent, defaultCurrencyCode(symbol));
	}

	private static String defaultCurrencyCode(String symbol) {
		if (symbol == null) {
			return "USD";
		}
		return symbol.trim().toUpperCase().matches("^[A-Z]{4,6}\\d{1,2}$") ? "BRL" : "USD";
	}
}
