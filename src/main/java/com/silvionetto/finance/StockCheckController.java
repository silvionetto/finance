package com.silvionetto.finance;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-check")
public class StockCheckController {

	private final StockCheckService stockCheckService;
	private final RequestSessionContext requestSessionContext;

	public StockCheckController(StockCheckService stockCheckService, RequestSessionContext requestSessionContext) {
		this.stockCheckService = stockCheckService;
		this.requestSessionContext = requestSessionContext;
	}

	@GetMapping
	public StockCheckSnapshot latest(HttpSession session) {
		return this.requestSessionContext.withSession(session.getId(), this.stockCheckService::getLatestSnapshot);
	}

	@PostMapping("/refresh")
	public StockCheckSnapshot refresh(HttpSession session) {
		return this.requestSessionContext.withSession(session.getId(), this.stockCheckService::refreshLatestSnapshot);
	}
}
