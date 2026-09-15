package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WalletRepository {
	List<WalletHolding> findAllByOwnerId(String ownerId);
	Optional<WalletHolding> findByOwnerIdAndSymbol(String ownerId, String symbol);
	WalletHolding save(
		String ownerId,
		String symbol,
		String companyName,
		BigDecimal quantity,
		BigDecimal averageCost,
		String currencyCode,
		Instant now
	);
	boolean deleteByOwnerIdAndSymbol(String ownerId, String symbol);
}
