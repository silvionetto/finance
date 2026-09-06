package com.silvionetto.finance;

import java.math.BigDecimal;

public record StockCheckItemResult(
	String symbol,
	String companyName,
	BigDecimal price,
	BigDecimal change,
	BigDecimal changePercent,
	String currencyCode,
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
		return new StockCheckItemResult(symbol, companyName, price, change, changePercent, currencyCode, recommendation, explanation, null);
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
		return success(symbol, companyName, price, change, changePercent, null, recommendation, explanation);
	}

	public static StockCheckItemResult failure(String symbol, String companyName, String error) {
		return new StockCheckItemResult(symbol, companyName, null, null, null, null, null, null, error);
	}
}
