package com.silvionetto.finance;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WatchlistTool {

	private final WatchlistService watchlistService;

	public WatchlistTool(WatchlistService watchlistService) {
		this.watchlistService = watchlistService;
	}

	@Tool(description = "List the current stock watchlist")
	public String listWatchlist() {
		List<WatchlistEntry> entries = this.watchlistService.listWatchlist();
		if (entries.isEmpty()) {
			return "Watchlist is empty.";
		}
		return entries.stream()
			.map(entry -> entry.symbol() + (entry.companyName() != null ? " (" + entry.companyName() + ")" : ""))
			.collect(Collectors.joining(", "));
	}

	@Tool(description = "Add a stock to the watchlist by ticker symbol or company name")
	public String addToWatchlist(String symbolOrCompanyName) {
		WatchlistEntry entry = this.watchlistService.addToWatchlist(symbolOrCompanyName);
		return "Added " + entry.symbol() + " to the watchlist.";
	}

	@Tool(description = "Remove a stock from the watchlist by ticker symbol or company name")
	public String removeFromWatchlist(String symbolOrCompanyName) {
		boolean removed = this.watchlistService.removeFromWatchlist(symbolOrCompanyName);
		return removed ? "Removed from watchlist." : "Stock was not on the watchlist.";
	}
}
