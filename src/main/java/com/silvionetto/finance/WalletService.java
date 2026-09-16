package com.silvionetto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

	private final WalletRepository walletRepository;
	private final TickerLookupTool tickerLookupTool;
	private final AuthenticatedUserContext authenticatedUserContext;

	public WalletService(
		WalletRepository walletRepository,
		TickerLookupTool tickerLookupTool,
		AuthenticatedUserContext authenticatedUserContext
	) {
		this.walletRepository = walletRepository;
		this.tickerLookupTool = tickerLookupTool;
		this.authenticatedUserContext = authenticatedUserContext;
	}

	public List<WalletHolding> listHoldings() {
		return this.walletRepository.findAllByOwnerId(currentOwnerId());
	}

	public WalletHolding saveHolding(
		String companyName,
		String symbol,
		BigDecimal quantity,
		BigDecimal averageCost,
		String currencyCode
	) {
		String normalizedCompanyName = normalizeCompanyName(companyName);
		String normalizedSymbol = normalizeSymbol(symbol);
		BigDecimal normalizedQuantity = requirePositiveWholeNumber(quantity, "quantity");
		BigDecimal normalizedAverageCost = requireNonNegative(averageCost, "averageCost");
		String normalizedCurrencyCode = normalizeCurrencyCode(currencyCode);

		return this.walletRepository.save(
			currentOwnerId(),
			normalizedSymbol,
			normalizedCompanyName,
			normalizedQuantity,
			normalizedAverageCost,
			normalizedCurrencyCode,
			Instant.now()
		);
	}

	public boolean removeHolding(String symbol) {
		return this.walletRepository.deleteByOwnerIdAndSymbol(currentOwnerId(), normalizeSymbol(symbol));
	}

	private String currentOwnerId() {
		return this.authenticatedUserContext.requireCurrentUsername();
	}

	private String normalizeCompanyName(String companyName) {
		if (companyName == null || companyName.isBlank()) {
			throw new IllegalArgumentException("companyName must not be blank");
		}
		return companyName.trim();
	}

	private String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		return this.tickerLookupTool.canonicalizeSymbol(symbol).trim().toUpperCase(Locale.ROOT);
	}

	private static BigDecimal requirePositiveWholeNumber(BigDecimal value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " must not be null");
		}
		BigDecimal normalized = value.stripTrailingZeros();
		if (normalized.signum() <= 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
		if (normalized.scale() > 0) {
			throw new IllegalArgumentException(fieldName + " must be a whole number");
		}
		return normalized;
	}

	private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " must not be null");
		}
		if (value.signum() < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		return value;
	}

	private static String normalizeCurrencyCode(String currencyCode) {
		if (currencyCode == null || currencyCode.isBlank()) {
			throw new IllegalArgumentException("currencyCode must not be blank");
		}
		String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
		if (!normalized.matches("[A-Z]{3}")) {
			throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code");
		}
		return normalized;
	}
}
