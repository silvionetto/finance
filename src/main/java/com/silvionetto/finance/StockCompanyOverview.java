package com.silvionetto.finance;

public record StockCompanyOverview(
	String symbol,
	String companyName,
	String market,
	String description,
	String sector,
	String industry,
	String country
) {
}
