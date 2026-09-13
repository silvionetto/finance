package com.silvionetto.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class StockRecommendationEngine {

	private static final BigDecimal SELL_THRESHOLD = new BigDecimal("5");
	private static final BigDecimal BUY_THRESHOLD = new BigDecimal("-5");

	public StockCheckItemResult recommend(WatchlistEntry entry, StockQuote quote) {
		String companyName = displayName(entry);
		BigDecimal changePercent = effectiveChangePercent(quote);

		if (changePercent == null) {
			return StockCheckItemResult.success(
				quote.symbol(),
				companyName,
				quote.price(),
				quote.change(),
				null,
				quote.currencyCode(),
				quote.quoteDate(),
				StockRecommendation.HOLD,
				"No percentage change was available, so hold for now."
			);
		}

		if (changePercent.compareTo(SELL_THRESHOLD) >= 0) {
			return StockCheckItemResult.success(
				quote.symbol(),
				companyName,
				quote.price(),
				quote.change(),
				changePercent,
				quote.currencyCode(),
				quote.quoteDate(),
				StockRecommendation.SELL,
				"Price is up %s%%, which is a strong move, so trim or sell if you want to lock in gains.".formatted(formatPercent(changePercent))
			);
		}

		if (changePercent.compareTo(BUY_THRESHOLD) <= 0) {
			return StockCheckItemResult.success(
				quote.symbol(),
				companyName,
				quote.price(),
				quote.change(),
				changePercent,
				quote.currencyCode(),
				quote.quoteDate(),
				StockRecommendation.BUY,
				"Price is down %s%%, which is a meaningful drop, so this is a buy/watch signal.".formatted(formatPercent(changePercent))
			);
		}

		return StockCheckItemResult.success(
			quote.symbol(),
			companyName,
			quote.price(),
			quote.change(),
			changePercent,
			quote.currencyCode(),
			quote.quoteDate(),
			StockRecommendation.HOLD,
			"Price moved %s%%, which is within the hold range.".formatted(formatPercent(changePercent))
		);
	}

	private static String displayName(WatchlistEntry entry) {
		if (entry.companyName() == null) {
			return null;
		}
		String companyName = entry.companyName().trim();
		if (companyName.isBlank() || companyName.equalsIgnoreCase(entry.symbol())) {
			return null;
		}
		return companyName;
	}

	private static String formatPercent(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private static BigDecimal effectiveChangePercent(StockQuote quote) {
		if (quote.changePercent() != null) {
			return quote.changePercent();
		}
		if (quote.price() == null || quote.change() == null) {
			return null;
		}

		BigDecimal previousClose = quote.price().subtract(quote.change());
		if (previousClose.compareTo(BigDecimal.ZERO) <= 0) {
			return null;
		}

		return quote.change()
			.multiply(BigDecimal.valueOf(100))
			.divide(previousClose, 10, java.math.RoundingMode.HALF_UP);
	}
}
