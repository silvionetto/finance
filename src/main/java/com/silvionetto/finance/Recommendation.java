package com.silvionetto.finance;

public record Recommendation(
	String tickerSymbol,
	String action,
	String reason
) {
}
