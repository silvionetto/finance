package com.silvionetto.finance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StockPageController {
	private final WatchlistService watchlistService;
	private final StockAnalysisService stockAnalysisService;

	public StockPageController(WatchlistService watchlistService, StockAnalysisService stockAnalysisService) {
		this.watchlistService = watchlistService;
		this.stockAnalysisService = stockAnalysisService;
	}

	@GetMapping("/stocks/{symbol}")
	public String stock(@PathVariable String symbol, Model model) {
		String normalized = symbol.trim().toUpperCase();
		WatchlistEntry entry = this.watchlistService.listWatchlist().stream()
			.filter(item -> item.symbol().equalsIgnoreCase(normalized)).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Symbol is not in the current watchlist: " + normalized));
		model.addAttribute("entry", entry);
		model.addAttribute("analyses", this.stockAnalysisService.list(normalized));
		return "stock";
	}
}
