package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WatchlistRepository {
	List<WatchlistEntry> findAllByOwnerId(String ownerId);
	Optional<WatchlistEntry> findByOwnerIdAndSymbol(String ownerId, String symbol);
	WatchlistEntry save(String ownerId, String symbol, String companyName, Instant now);
	boolean deleteByOwnerIdAndSymbol(String ownerId, String symbol);
}
