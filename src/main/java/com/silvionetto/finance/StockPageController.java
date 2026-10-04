package com.silvionetto.finance;

import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StockPageController {
	private final WatchlistService watchlistService;
	private final StockAnalysisService stockAnalysisService;
	private final StockCompanyOverviewService stockCompanyOverviewService;

	public StockPageController(
		WatchlistService watchlistService,
		StockAnalysisService stockAnalysisService,
		StockCompanyOverviewService stockCompanyOverviewService
	) {
		this.watchlistService = watchlistService;
		this.stockAnalysisService = stockAnalysisService;
		this.stockCompanyOverviewService = stockCompanyOverviewService;
	}

	@GetMapping("/stocks/{symbol}")
	public String stock(@PathVariable String symbol, Model model) {
		String normalized = symbol.trim().toUpperCase(Locale.ROOT);
		WatchlistEntry entry = this.watchlistService.listWatchlist().stream()
			.filter(item -> item.symbol().equalsIgnoreCase(normalized)).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Symbol is not in the current watchlist: " + normalized));
		model.addAttribute("entry", entry);
		model.addAttribute("companyOverview", this.stockCompanyOverviewService.build(entry));
		model.addAttribute("analyses", this.stockAnalysisService.list(normalized));
		return "stock";
	}
}
