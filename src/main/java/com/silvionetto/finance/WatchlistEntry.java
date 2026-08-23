package com.silvionetto.finance;

import java.time.Instant;

public record WatchlistEntry(
	String ownerId,
	String symbol,
	String companyName,
	Instant createdAt,
	Instant updatedAt
) {
}
