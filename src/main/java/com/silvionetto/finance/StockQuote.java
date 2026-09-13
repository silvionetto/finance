package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockQuote(
	String symbol,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent,
	String currencyCode,
	LocalDate quoteDate
) {
	public StockQuote(String symbol, BigDecimal price, BigDecimal change, BigDecimal changePercent) {
		this(symbol, price, change, changePercent, MarketSymbolSupport.defaultCurrencyCode(symbol), null);
	}

	public StockQuote(String symbol, BigDecimal price, BigDecimal change, BigDecimal changePercent, String currencyCode) {
		this(symbol, price, change, changePercent, currencyCode, null);
	}
}
