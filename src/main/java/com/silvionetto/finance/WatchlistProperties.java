package com.silvionetto.finance;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watchlist")
public record WatchlistProperties(List<Item> items) {

	public record Item(
		String companyName,
		String tickerSymbol,
		String exchange,
		BigDecimal targetPrice,
		BigDecimal stopLoss,
		String notes
	) {
	}
}
