package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StockRecommendationEngineTests {

	@Test
	void recommendsSellWhenPriceIsUpStrongly() {
		StockRecommendationEngine engine = new StockRecommendationEngine();
		WatchlistEntry entry = new WatchlistEntry("default", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH);
		StockCheckItemResult result = engine.recommend(entry, new StockQuote("AAPL", new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("6")));

		assertThat(result.recommendation()).isEqualTo(StockRecommendation.SELL);
		assertThat(result.explanation()).contains("trim or sell");
	}

	@Test
	void recommendsBuyWhenPriceIsDownStrongly() {
		StockRecommendationEngine engine = new StockRecommendationEngine();
		WatchlistEntry entry = new WatchlistEntry("default", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH);
		StockCheckItemResult result = engine.recommend(entry, new StockQuote("AAPL", new BigDecimal("200"), new BigDecimal("-10"), new BigDecimal("-6")));

		assertThat(result.recommendation()).isEqualTo(StockRecommendation.BUY);
		assertThat(result.explanation()).contains("buy/watch signal");
	}

	@Test
	void recommendsHoldWhenMovementIsSmall() {
		StockRecommendationEngine engine = new StockRecommendationEngine();
		WatchlistEntry entry = new WatchlistEntry("default", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH);
		StockCheckItemResult result = engine.recommend(entry, new StockQuote("AAPL", new BigDecimal("200"), new BigDecimal("1"), new BigDecimal("1")));

		assertThat(result.recommendation()).isEqualTo(StockRecommendation.HOLD);
		assertThat(result.explanation()).contains("hold range");
	}

	@Test
	void computesChangePercentWhenApiDoesNotProvideOne() {
		StockRecommendationEngine engine = new StockRecommendationEngine();
		WatchlistEntry entry = new WatchlistEntry("default", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH);
		StockCheckItemResult result = engine.recommend(entry, new StockQuote("AAPL", new BigDecimal("327.45"), new BigDecimal("2.49"), null));

		assertThat(result.changePercent()).isEqualByComparingTo("0.7662481536");
		assertThat(result.recommendation()).isEqualTo(StockRecommendation.HOLD);
		assertThat(result.explanation()).contains("hold range");
	}
}
