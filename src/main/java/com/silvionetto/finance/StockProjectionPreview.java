package com.silvionetto.finance;

import java.time.Instant;

public record StockProjectionPreview(
	Long id,
	String ownerId,
	String tickerSymbol,
	String content,
	Instant createdAt
) {}
