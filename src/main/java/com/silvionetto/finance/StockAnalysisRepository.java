package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;

public interface StockAnalysisRepository {
	StockAnalysis save(String ownerId, String symbol, String companyName, StockAnalysisType type, String generatedOutput,
		java.math.BigDecimal baselinePrice, java.math.BigDecimal baselineChange,
		java.math.BigDecimal baselineChangePercent, String baselineCurrencyCode, Instant createdAt);
	List<StockAnalysis> findAllByOwnerIdAndSymbol(String ownerId, String symbol);
}
