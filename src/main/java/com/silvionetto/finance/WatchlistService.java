package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

	private final WatchlistRepository watchlistRepository;
	private final TickerLookupTool tickerLookupTool;
	private final AuthenticatedUserContext authenticatedUserContext;

	public WatchlistService(
		WatchlistRepository watchlistRepository,
		TickerLookupTool tickerLookupTool,
		AuthenticatedUserContext authenticatedUserContext
	) {
		this.watchlistRepository = watchlistRepository;
		this.tickerLookupTool = tickerLookupTool;
		this.authenticatedUserContext = authenticatedUserContext;
	}

	public List<WatchlistEntry> listWatchlist() {
		return this.watchlistRepository.findAllByOwnerId(currentOwnerId());
	}

	public WatchlistEntry addToWatchlist(String symbolOrCompanyName) {
		String symbol = resolveSymbol(symbolOrCompanyName);
		String normalized = normalize(symbol);
		String companyName = symbolOrCompanyName == null ? null : symbolOrCompanyName.trim();
		return this.watchlistRepository.save(currentOwnerId(), normalized, companyName, Instant.now());
	}

	public boolean removeFromWatchlist(String symbolOrCompanyName) {
		String symbol = resolveSymbol(symbolOrCompanyName);
		return this.watchlistRepository.deleteByOwnerIdAndSymbol(currentOwnerId(), normalize(symbol));
	}

	private String currentOwnerId() {
		return this.authenticatedUserContext.requireCurrentUsername();
	}

	private String resolveSymbol(String symbolOrCompanyName) {
		if (symbolOrCompanyName == null || symbolOrCompanyName.isBlank()) {
			throw new IllegalArgumentException("symbolOrCompanyName must not be blank");
		}
		String trimmed = symbolOrCompanyName.trim();
		if (trimmed.matches("[A-Za-z0-9.\\-]+")) {
			return this.tickerLookupTool.canonicalizeSymbol(trimmed);
		}
		return this.tickerLookupTool.lookupTickerSymbol(trimmed);
	}

	private static String normalize(String symbol) {
		return symbol.trim().toUpperCase(Locale.ROOT);
	}
}
