package com.silvionetto.finance;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

	private final WatchlistService watchlistService;

	public WatchlistController(WatchlistService watchlistService) {
		this.watchlistService = watchlistService;
	}

	@GetMapping
	public List<WatchlistEntry> list() {
		return this.watchlistService.listWatchlist();
	}

	@PostMapping
	public WatchlistEntry add(@RequestBody WatchlistItemRequest request) {
		return this.watchlistService.addToWatchlist(request.symbolOrCompanyName());
	}

	@DeleteMapping
	public void remove(@RequestBody WatchlistItemRequest request) {
		this.watchlistService.removeFromWatchlist(request.symbolOrCompanyName());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> handleStatusException(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode())
			.body(new ApiError(exception.getBody().getDetail()));
	}

	public record ApiError(String message) {}

	public record WatchlistItemRequest(String symbolOrCompanyName) {}
}
