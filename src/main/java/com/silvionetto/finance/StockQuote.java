package com.silvionetto.finance;

import java.math.BigDecimal;

public record StockQuote(
	String symbol,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent,
	String currencyCode
) {
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
