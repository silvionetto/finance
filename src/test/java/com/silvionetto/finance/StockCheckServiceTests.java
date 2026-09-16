package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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

		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("user", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH)));
		when(tickerLookupTool.fetchQuote("AAPL")).thenReturn(new StockQuote("AAPL", new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("6")));

		StockCheckSnapshot snapshot = requestSessionContext.withSession("session-1", service::refreshLatestSnapshot);

		assertThat(snapshot.results()).hasSize(1);
		assertThat(snapshot.results().getFirst().recommendation()).isEqualTo(StockRecommendation.SELL);
		assertThat(snapshot.results().getFirst().currencyCode()).isEqualTo("USD");
	}

	@Test
	void refreshLatestSnapshotCapturesErrorsPerItem() {
		WatchlistService watchlistService = mock(WatchlistService.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		StockRecommendationEngine engine = new StockRecommendationEngine();
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		StockCheckService service = new StockCheckService(watchlistService, tickerLookupTool, engine, requestSessionContext);

		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("user", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH)));
		when(tickerLookupTool.fetchQuote("AAPL")).thenThrow(new IllegalStateException("Financial Modeling Prep API key is not configured"));

		StockCheckSnapshot snapshot = requestSessionContext.withSession("session-1", service::refreshLatestSnapshot);

		assertThat(snapshot.results()).hasSize(1);
		assertThat(snapshot.results().getFirst().error()).contains("API key is not configured");
	}

	@Test
	void refreshLatestSnapshotPreservesEuronextCurrencyAndSymbol() {
		WatchlistService watchlistService = mock(WatchlistService.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		StockRecommendationEngine engine = new StockRecommendationEngine();
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		StockCheckService service = new StockCheckService(watchlistService, tickerLookupTool, engine, requestSessionContext);

		when(watchlistService.listWatchlist()).thenReturn(List.of(new WatchlistEntry("user", "ABN.AS", "ABN AMRO", Instant.EPOCH, Instant.EPOCH)));
		when(tickerLookupTool.fetchQuote("ABN.AS")).thenReturn(new StockQuote("ABN.AS", new BigDecimal("43.08"), new BigDecimal("-0.44"), new BigDecimal("-1.0101102941"), "EUR"));

		StockCheckSnapshot snapshot = requestSessionContext.withSession("session-1", service::refreshLatestSnapshot);

		assertThat(snapshot.results()).hasSize(1);
		assertThat(snapshot.results().getFirst().symbol()).isEqualTo("ABN.AS");
		assertThat(snapshot.results().getFirst().currencyCode()).isEqualTo("EUR");
	}
}
