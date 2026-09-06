package com.silvionetto.finance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class StockCheckService {

	private final WatchlistService watchlistService;
	private final TickerLookupTool tickerLookupTool;
	private final StockRecommendationEngine recommendationEngine;
	private final RequestSessionContext requestSessionContext;
	private final Map<String, StockCheckSnapshot> latestSnapshotsBySessionId = new HashMap<>();

	public StockCheckService(
		WatchlistService watchlistService,
		TickerLookupTool tickerLookupTool,
		StockRecommendationEngine recommendationEngine,
		RequestSessionContext requestSessionContext
	) {
		this.watchlistService = watchlistService;
		this.tickerLookupTool = tickerLookupTool;
		this.recommendationEngine = recommendationEngine;
		this.requestSessionContext = requestSessionContext;
	}

	public synchronized StockCheckSnapshot getLatestSnapshot() {
		return this.latestSnapshotsBySessionId.computeIfAbsent(currentSessionId(), ignored -> buildSnapshot());
	}

	public synchronized StockCheckSnapshot refreshLatestSnapshot() {
		StockCheckSnapshot snapshot = buildSnapshot();
		this.latestSnapshotsBySessionId.put(currentSessionId(), snapshot);
		return snapshot;
	}

	private StockCheckSnapshot buildSnapshot() {
		List<WatchlistEntry> watchlistEntries = this.watchlistService.listWatchlist();
		List<StockCheckItemResult> results = new ArrayList<>(watchlistEntries.size());

		for (WatchlistEntry entry : watchlistEntries) {
			try {
				StockQuote quote = this.tickerLookupTool.fetchQuote(entry.symbol());
				results.add(this.recommendationEngine.recommend(entry, quote));
			} catch (RestClientException | IllegalArgumentException | IllegalStateException ex) {
				results.add(StockCheckItemResult.failure(entry.symbol(), entry.companyName(), ex.getMessage()));
			}
		}

		return new StockCheckSnapshot(Instant.now(), List.copyOf(results));
	}

	private String currentSessionId() {
		return this.requestSessionContext.requireCurrentSessionId();
	}
}
