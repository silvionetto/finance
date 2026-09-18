package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record StockAnalysis(
	Long id,
	String ownerId,
	String symbol,
	String companyName,
	StockAnalysisType type,
	String generatedOutput,
	BigDecimal baselinePrice,
	BigDecimal baselineChange,
	BigDecimal baselineChangePercent,
	String baselineCurrencyCode,
	Instant createdAt
) {
	public StockAnalysis(Long id, String ownerId, String symbol, String companyName, String generatedOutput,
			BigDecimal baselinePrice, BigDecimal baselineChange, BigDecimal baselineChangePercent,
			String baselineCurrencyCode, Instant createdAt) {
		this(id, ownerId, symbol, companyName, StockAnalysisType.ANALYSIS, generatedOutput, baselinePrice,
			baselineChange, baselineChangePercent, baselineCurrencyCode, createdAt);
	}
}
