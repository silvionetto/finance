package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;

public record StockCheckSnapshot(
	Instant checkedAt,
	List<StockCheckItemResult> results
) {
}
