package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockCheckItemResult(
	String symbol,
	String companyName,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent,
	String currencyCode,
	LocalDate quoteDate,
	StockRecommendation recommendation,
	String explanation,
	String error
) {
	public static StockCheckItemResult success(
		String symbol,
		String companyName,
		BigDecimal price,
		BigDecimal change,
		BigDecimal changePercent,
		String currencyCode,
		StockRecommendation recommendation,
		String explanation
	) {
		return success(symbol, companyName, price, change, changePercent, currencyCode, null, recommendation, explanation);
	}

	public static StockCheckItemResult success(
		String symbol,
		String companyName,
		BigDecimal price,
		BigDecimal change,
		BigDecimal changePercent,
		String currencyCode,
		LocalDate quoteDate,
		StockRecommendation recommendation,
		String explanation
	) {
		return new StockCheckItemResult(symbol, companyName, price, change, changePercent, currencyCode, quoteDate, recommendation, explanation, null);
	}

	public static StockCheckItemResult success(
		String symbol,
		String companyName,
		BigDecimal price,
		BigDecimal change,
		BigDecimal changePercent,
		StockRecommendation recommendation,
		String explanation
	) {
		return success(symbol, companyName, price, change, changePercent, null, null, recommendation, explanation);
	}

	public static StockCheckItemResult failure(String symbol, String companyName, String error) {
		return new StockCheckItemResult(symbol, companyName, null, null, null, null, null, null, null, error);
	}
}
