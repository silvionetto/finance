package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StockQuoteTests {

	@Test
	void defaultsAmsterdamSymbolsToEuro() {
		StockQuote quote = new StockQuote("ASML.AS", new BigDecimal("675.40"), new BigDecimal("4.10"), new BigDecimal("0.61"));

		assertThat(quote.currencyCode()).isEqualTo("EUR");
		assertThat(quote.quoteDate()).isNull();
	}

	@Test
	void defaultsBrazilianSymbolsToBrl() {
		StockQuote quote = new StockQuote("PETR4", new BigDecimal("41.18"), new BigDecimal("-0.58"), new BigDecimal("-1.39"));

		assertThat(quote.currencyCode()).isEqualTo("BRL");
		assertThat(quote.quoteDate()).isNull();
	}

	@Test
	void storesQuoteDateWhenProvided() {
		LocalDate quoteDate = LocalDate.of(2026, 9, 11);

		StockQuote quote = new StockQuote("ASML.AS", new BigDecimal("675.40"), new BigDecimal("4.10"), new BigDecimal("0.61"), "EUR", quoteDate);

		assertThat(quote.quoteDate()).isEqualTo(quoteDate);
	}
}
