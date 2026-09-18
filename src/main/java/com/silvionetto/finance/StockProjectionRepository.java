package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockProjectionRepository {
	StockProjectionPreview save(String ownerId, String tickerSymbol, String content, Instant createdAt);
	List<StockProjectionPreview> findAllByOwnerId(String ownerId);
	Optional<StockProjectionPreview> findLatestByOwnerIdAndTicker(String ownerId, String tickerSymbol);
}
