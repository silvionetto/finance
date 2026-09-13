package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockCheckServiceTests {

	@Test
	void refreshLatestSnapshotBuildsRecommendations() {
		WatchlistService watchlistService = mock(WatchlistService.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		StockRecommendationEngine engine = new StockRecommendationEngine();
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		StockCheckService service = new StockCheckService(watchlistService, tickerLookupTool, engine, requestSessionContext);
		LocalDate quoteDate = LocalDate.of(2026, 9, 11);

		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("default", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH)));
		when(tickerLookupTool.fetchQuote("AAPL")).thenReturn(new StockQuote("AAPL", new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("6"), "USD", quoteDate));

		StockCheckSnapshot snapshot = requestSessionContext.withSession("session-1", service::refreshLatestSnapshot);

		assertThat(snapshot.results()).hasSize(1);
		assertThat(snapshot.results().getFirst().recommendation()).isEqualTo(StockRecommendation.SELL);
		assertThat(snapshot.results().getFirst().currencyCode()).isEqualTo("USD");
		assertThat(snapshot.results().getFirst().quoteDate()).isEqualTo(quoteDate);
	}

	@Test
	void refreshLatestSnapshotCapturesErrorsPerItem() {
		WatchlistService watchlistService = mock(WatchlistService.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		StockRecommendationEngine engine = new StockRecommendationEngine();
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		StockCheckService service = new StockCheckService(watchlistService, tickerLookupTool, engine, requestSessionContext);

		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("default", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH)));
		when(tickerLookupTool.fetchQuote("AAPL")).thenThrow(new IllegalStateException("Financial Modeling Prep API key is not configured"));

		StockCheckSnapshot snapshot = requestSessionContext.withSession("session-1", service::refreshLatestSnapshot);

		assertThat(snapshot.results()).hasSize(1);
		assertThat(snapshot.results().getFirst().error()).contains("API key is not configured");
	}
}
