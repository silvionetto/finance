package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WalletServiceTests {

	@Test
	void listHoldingsUsesCurrentSessionId() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WalletService service = new WalletService(repository, tickerLookupTool, requestSessionContext);
		when(repository.findAllByOwnerId("session-1")).thenReturn(List.of(
			new WalletHolding("session-1", "AAPL", "Apple Inc.", new BigDecimal("4"), new BigDecimal("189.30"), "USD", Instant.EPOCH, Instant.EPOCH)
		));

		List<WalletHolding> holdings = requestSessionContext.withSession("session-1", service::listHoldings);

		assertThat(holdings).hasSize(1);
		verify(repository).findAllByOwnerId("session-1");
	}

	@Test
	void saveHoldingNormalizesInputs() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WalletService service = new WalletService(repository, tickerLookupTool, requestSessionContext);
		when(tickerLookupTool.canonicalizeSymbol(" aapl ")).thenReturn("AAPL");
		when(repository.save(
			eq("session-1"),
			eq("AAPL"),
			eq("Apple Inc."),
			eq(new BigDecimal("4.25")),
			eq(new BigDecimal("189.30")),
			eq("USD"),
			any()
		)).thenReturn(new WalletHolding(
			"session-1",
			"AAPL",
			"Apple Inc.",
			new BigDecimal("4.25"),
			new BigDecimal("189.30"),
			"USD",
			Instant.EPOCH,
			Instant.EPOCH
		));

		WalletHolding holding = requestSessionContext.withSession(
			"session-1",
			() -> service.saveHolding(" Apple Inc. ", " aapl ", new BigDecimal("4.25"), new BigDecimal("189.30"), "usd")
		);

		assertThat(holding.symbol()).isEqualTo("AAPL");
		assertThat(holding.currencyCode()).isEqualTo("USD");
		verify(repository).save(
			eq("session-1"),
			eq("AAPL"),
			eq("Apple Inc."),
			eq(new BigDecimal("4.25")),
			eq(new BigDecimal("189.30")),
			eq("USD"),
			any()
		);
	}

	@Test
	void removeHoldingUsesCurrentSessionId() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WalletService service = new WalletService(repository, tickerLookupTool, requestSessionContext);
		when(tickerLookupTool.canonicalizeSymbol("AAPL")).thenReturn("AAPL");
		when(repository.deleteByOwnerIdAndSymbol("session-2", "AAPL")).thenReturn(true);

		boolean removed = requestSessionContext.withSession("session-2", () -> service.removeHolding("AAPL"));

		assertThat(removed).isTrue();
		verify(repository).deleteByOwnerIdAndSymbol("session-2", "AAPL");
	}

	@Test
	void saveHoldingRejectsBlankCompanyName() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		RequestSessionContext requestSessionContext = new RequestSessionContext();
		WalletService service = new WalletService(repository, tickerLookupTool, requestSessionContext);

		assertThatThrownBy(() -> requestSessionContext.withSession(
			"session-1",
			() -> service.saveHolding(" ", "AAPL", BigDecimal.ONE, new BigDecimal("189.30"), "USD")
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("companyName must not be blank");
	}
}
