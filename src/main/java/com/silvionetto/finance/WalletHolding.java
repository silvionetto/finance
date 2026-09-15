package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletHolding(
	String ownerId,
	String symbol,
	String companyName,
	BigDecimal quantity,
	BigDecimal averageCost,
	String currencyCode,
	Instant createdAt,
	Instant updatedAt
) {
}
