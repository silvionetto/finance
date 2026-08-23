package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

	public static final String DEFAULT_OWNER_ID = "default";

	private final WatchlistRepository watchlistRepository;
	private final TickerLookupTool tickerLookupTool;

	public WatchlistService(WatchlistRepository watchlistRepository, TickerLookupTool tickerLookupTool) {
		this.watchlistRepository = watchlistRepository;
		this.tickerLookupTool = tickerLookupTool;
	}

	public List<WatchlistEntry> listWatchlist() {
		return this.watchlistRepository.findAllByOwnerId(DEFAULT_OWNER_ID);
	}

	public WatchlistEntry addToWatchlist(String symbolOrCompanyName) {
		String symbol = resolveSymbol(symbolOrCompanyName);
		String normalized = normalize(symbol);
		String companyName = symbolOrCompanyName == null ? null : symbolOrCompanyName.trim();
		return this.watchlistRepository.save(DEFAULT_OWNER_ID, normalized, companyName, Instant.now());
	}

	public boolean removeFromWatchlist(String symbolOrCompanyName) {
		String symbol = resolveSymbol(symbolOrCompanyName);
		return this.watchlistRepository.deleteByOwnerIdAndSymbol(DEFAULT_OWNER_ID, normalize(symbol));
	}

	private String resolveSymbol(String symbolOrCompanyName) {
		if (symbolOrCompanyName == null || symbolOrCompanyName.isBlank()) {
			throw new IllegalArgumentException("symbolOrCompanyName must not be blank");
		}
		String trimmed = symbolOrCompanyName.trim();
		if (trimmed.matches("[A-Za-z0-9.\\-]+")) {
			return trimmed;
		}
		return this.tickerLookupTool.lookupTickerSymbol(trimmed);
	}

	private static String normalize(String symbol) {
		return symbol.trim().toUpperCase(Locale.ROOT);
	}
}
