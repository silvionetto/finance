package com.silvionetto.finance;

import java.math.BigDecimal;

public record WatchlistItem(
	String companyName,
	String tickerSymbol,
	String exchange,
	BigDecimal targetPrice,
	BigDecimal stopLoss,
	String notes
) {
}
