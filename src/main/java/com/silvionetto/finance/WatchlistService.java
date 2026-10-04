package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatusCode;

@Service
public class WatchlistService {

	private static final Logger LOGGER = LoggerFactory.getLogger(WatchlistService.class);

	private final WatchlistRepository watchlistRepository;
	private final TickerLookupTool tickerLookupTool;
	private final CompanyProfileService companyProfileService;
	private final CompanyTickerCatalog companyTickerCatalog;
	private final AuthenticatedUserContext authenticatedUserContext;

	public WatchlistService(
		WatchlistRepository watchlistRepository,
		TickerLookupTool tickerLookupTool,
		CompanyProfileService companyProfileService,
		CompanyTickerCatalog companyTickerCatalog,
		AuthenticatedUserContext authenticatedUserContext
	) {
		this.watchlistRepository = watchlistRepository;
		this.tickerLookupTool = tickerLookupTool;
		this.companyProfileService = companyProfileService;
		this.companyTickerCatalog = companyTickerCatalog;
		this.authenticatedUserContext = authenticatedUserContext;
	}

	public List<WatchlistEntry> listWatchlist() {
		String ownerId = currentOwnerId();
		return this.watchlistRepository.findAllByOwnerId(ownerId).stream()
			.map(entry -> enrichLegacyEntry(ownerId, entry))
			.toList();
	}

	public WatchlistEntry addToWatchlist(String symbolOrCompanyName) {
		String symbol;
		try {
			symbol = resolveSymbol(symbolOrCompanyName);
		} catch (IllegalArgumentException | IllegalStateException ex) {
			throw new ResponseStatusException(
				HttpStatusCode.valueOf(422),
				"Unable to identify a company for the supplied ticker or name: " + ex.getMessage(),
				ex
			);
		}
		String normalized = normalize(symbol);
		CompanyProfile profile;
		try {
			profile = this.companyProfileService.findProfile(normalized).orElse(null);
		} catch (RestClientException | IllegalStateException ex) {
			throw new ResponseStatusException(
				HttpStatusCode.valueOf(422),
				"Unable to retrieve company details for " + normalized + ". Please try again later.",
				ex
			);
		}
		String companyName = companyName(profile, normalized);
		String description = profile == null ? null : profile.description();
		if (companyName == null || description == null || description.isBlank()) {
			throw new ResponseStatusException(
				HttpStatusCode.valueOf(422),
				"Company name and description could not be found for " + normalized + ". The ticker was not added."
			);
		}
		return this.watchlistRepository.save(currentOwnerId(), normalized, companyName, description, Instant.now());
	}

	public boolean removeFromWatchlist(String symbolOrCompanyName) {
		String symbol = resolveSymbol(symbolOrCompanyName);
		return this.watchlistRepository.deleteByOwnerIdAndSymbol(currentOwnerId(), normalize(symbol));
	}

	private String currentOwnerId() {
		return this.authenticatedUserContext.requireCurrentUsername();
	}

	private WatchlistEntry enrichLegacyEntry(String ownerId, WatchlistEntry entry) {
		if (hasUsefulName(entry.companyName(), entry.symbol())
			&& entry.companyDescription() != null && !entry.companyDescription().isBlank()) {
			return entry;
		}

		try {
			CompanyProfile profile = this.companyProfileService.findProfile(entry.symbol()).orElse(null);
			String companyName = companyName(profile, entry.symbol());
			String description = profile == null ? null : profile.description();
			if (companyName == null || description == null || description.isBlank()) {
				return entry;
			}
			return this.watchlistRepository.updateCompanyDetails(ownerId, entry.symbol(), companyName, description, Instant.now());
		} catch (RestClientException | IllegalStateException ex) {
			LOGGER.warn("Unable to enrich saved watchlist entry for {}", entry.symbol(), ex);
			return entry;
		}
	}

	private String companyName(CompanyProfile profile, String symbol) {
		if (profile != null && hasUsefulName(profile.companyName(), symbol)) {
			return profile.companyName().trim();
		}
		return this.companyTickerCatalog.findByTickerSymbol(symbol)
			.map(CompanyTickerCatalog.CompanyTickerEntry::company_name)
			.filter(name -> hasUsefulName(name, symbol))
			.map(String::trim)
			.orElse(null);
	}

	private static boolean hasUsefulName(String name, String symbol) {
		return name != null && !name.isBlank() && !name.trim().equalsIgnoreCase(symbol);
	}

	private String resolveSymbol(String symbolOrCompanyName) {
		if (symbolOrCompanyName == null || symbolOrCompanyName.isBlank()) {
			throw new IllegalArgumentException("symbolOrCompanyName must not be blank");
		}
		String trimmed = symbolOrCompanyName.trim();
		if (looksLikeTicker(trimmed)) {
			return this.tickerLookupTool.canonicalizeSymbol(trimmed);
		}
		return this.tickerLookupTool.lookupTickerSymbol(trimmed);
	}

	private static boolean looksLikeTicker(String value) {
		if (!value.matches("[A-Za-z0-9.\\-]+")) {
			return false;
		}
		if (value.contains(".") || value.contains("-")) {
			return true;
		}
		return value.equals(value.toUpperCase(Locale.ROOT))
			|| value.equals(value.toLowerCase(Locale.ROOT));
	}

	private static String normalize(String symbol) {
		return symbol.trim().toUpperCase(Locale.ROOT);
	}
}
