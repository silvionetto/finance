package com.silvionetto.finance;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockAnalysisController {
	private final StockAnalysisService stockAnalysisService;

	public StockAnalysisController(StockAnalysisService stockAnalysisService) {
		this.stockAnalysisService = stockAnalysisService;
	}

	@GetMapping("/{symbol}/analyses")
	public List<StockAnalysis> list(@PathVariable String symbol) {
		return this.stockAnalysisService.list(symbol);
	}

	@PostMapping("/{symbol}/analyses")
	public StockAnalysis create(@PathVariable String symbol) {
		return this.stockAnalysisService.create(symbol);
	}

	@GetMapping("/{symbol}/comparison")
	public StockAnalysisService.StockComparison comparison(@PathVariable String symbol) {
		return this.stockAnalysisService.comparison(symbol);
	}
}
