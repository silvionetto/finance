package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class StockPageControllerTests {

	@Test
	void addsCompanyOverviewToStockPageModel() {
		WatchlistService watchlistService = mock(WatchlistService.class);
		StockAnalysisService analysisService = mock(StockAnalysisService.class);
		StockCompanyOverviewService overviewService = mock(StockCompanyOverviewService.class);
		WatchlistEntry entry = new WatchlistEntry("owner", "AAPL", "Apple Inc.", Instant.EPOCH, Instant.EPOCH);
		StockCompanyOverview overview = new StockCompanyOverview(
			"AAPL", "Apple Inc.", "NASDAQ", "Company description", "Technology", "Consumer Electronics", "US"
		);
		when(watchlistService.listWatchlist()).thenReturn(List.of(entry));
		when(overviewService.build(entry)).thenReturn(overview);
		when(analysisService.list("AAPL")).thenReturn(List.of());
		StockPageController controller = new StockPageController(watchlistService, analysisService, overviewService);
		ExtendedModelMap model = new ExtendedModelMap();

		String view = controller.stock("aapl", model);

		assertThat(view).isEqualTo("stock");
		assertThat(model.get("companyOverview")).isEqualTo(overview);
		assertThat(model.get("analyses")).isEqualTo(List.of());
	}
}
