package com.silvionetto.finance;

import java.util.Locale;
import java.util.regex.Pattern;

final class MarketSymbolSupport {

	private static final Pattern BRAZILIAN_SYMBOL = Pattern.compile("^[A-Z]{4,6}\\d{1,2}$");

	private MarketSymbolSupport() {
	}

	static String normalize(String symbol) {
		if (symbol == null) {
			return "";
		}
		return symbol.trim().toUpperCase(Locale.ROOT);
	}

	static boolean isBrazilianSymbol(String symbol) {
		String normalized = normalize(symbol).replace(".SA", "");
		return BRAZILIAN_SYMBOL.matcher(normalized).matches();
	}

	static boolean isAmsterdamSymbol(String symbol) {
		return normalize(symbol).endsWith(".AS");
	}

	static String defaultCurrencyCode(String symbol) {
		if (isBrazilianSymbol(symbol)) {
			return "BRL";
		}
		if (isAmsterdamSymbol(symbol)) {
			return "EUR";
		}
		return "USD";
	}
}
