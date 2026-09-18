package com.silvionetto.finance;

import java.time.Instant;
import java.util.List;

public interface MarketObservationRepository {
	MarketObservation save(MarketObservation observation);
	List<MarketObservation> findAllByOwnerIdAndSymbol(String ownerId, String symbol);
}
