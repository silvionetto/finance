package com.silvionetto.finance;

import java.time.Instant;

public record WatchlistEntry(
	String ownerId,
	String symbol,
	String companyName,
	String companyDescription,
	Instant createdAt,
	Instant updatedAt
) {
	public WatchlistEntry(
		String ownerId,
		String symbol,
		String companyName,
		Instant createdAt,
		Instant updatedAt
	) {
		this(ownerId, symbol, companyName, null, createdAt, updatedAt);
	}
}
