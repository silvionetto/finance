package com.silvionetto.finance;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/stock-projection", "/api/projections", "/api/projection"})
public class StockProjectionController {

	private final StockProjectionService stockProjectionService;

	public StockProjectionController(StockProjectionService stockProjectionService) {
		this.stockProjectionService = stockProjectionService;
	}

	@PostMapping
	public StockProjectionPreview generate(@RequestBody StockProjectionRequest request) {
		return this.stockProjectionService.generateProjection(request.resolveTicker());
	}

	@GetMapping
	public List<StockProjectionPreview> list() {
		return this.stockProjectionService.listRecent();
	}

	@GetMapping("/latest")
	public StockProjectionPreview latest(@RequestParam(value = "ticker", required = false) String ticker) {
		if (ticker == null || ticker.isBlank()) {
			return this.stockProjectionService.listRecent().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("No projection previews have been generated yet"));
		}
		return this.stockProjectionService.latestForTicker(ticker);
	}

	@GetMapping("/{ticker}")
	public StockProjectionPreview latestForTicker(@PathVariable String ticker) {
		return this.stockProjectionService.latestForTicker(ticker);
	}

	public static class StockProjectionRequest {
		@JsonAlias({"tickerSymbol", "symbol"})
		private String ticker;

		public String getTicker() {
			return this.ticker;
		}

		public void setTicker(String ticker) {
			this.ticker = ticker;
		}

		public String resolveTicker() {
			if (this.ticker != null && !this.ticker.isBlank()) {
				return this.ticker;
			}
			throw new IllegalArgumentException("ticker must not be blank");
		}
	}
}
