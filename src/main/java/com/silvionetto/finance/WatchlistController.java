package com.silvionetto.finance;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

	private final WatchlistService watchlistService;
	private final RequestSessionContext requestSessionContext;

	public WatchlistController(WatchlistService watchlistService, RequestSessionContext requestSessionContext) {
		this.watchlistService = watchlistService;
		this.requestSessionContext = requestSessionContext;
	}

	@GetMapping
	public List<WatchlistEntry> list(HttpSession session) {
		return this.requestSessionContext.withSession(session.getId(), this.watchlistService::listWatchlist);
	}

	@PostMapping
	public WatchlistEntry add(HttpSession session, @RequestBody WatchlistItemRequest request) {
		return this.requestSessionContext.withSession(session.getId(), () -> this.watchlistService.addToWatchlist(request.symbolOrCompanyName()));
	}

	@DeleteMapping
	public void remove(HttpSession session, @RequestBody WatchlistItemRequest request) {
		this.requestSessionContext.withSession(session.getId(), () -> this.watchlistService.removeFromWatchlist(request.symbolOrCompanyName()));
	}

	public record WatchlistItemRequest(String symbolOrCompanyName) {}
}
