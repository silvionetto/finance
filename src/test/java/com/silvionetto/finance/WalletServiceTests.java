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
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WalletService service = new WalletService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");
		when(repository.findAllByOwnerId("user")).thenReturn(List.of(
			new WalletHolding("user", "AAPL", "Apple Inc.", new BigDecimal("4"), new BigDecimal("189.30"), "USD", Instant.EPOCH, Instant.EPOCH)
		));

		List<WalletHolding> holdings = service.listHoldings();

		assertThat(holdings).hasSize(1);
		verify(repository).findAllByOwnerId("user");
	}

	@Test
	void saveHoldingNormalizesInputs() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WalletService service = new WalletService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");
		when(tickerLookupTool.canonicalizeSymbol(" aapl ")).thenReturn("AAPL");
		when(repository.save(
			eq("user"),
			eq("AAPL"),
			eq("Apple Inc."),
			eq(new BigDecimal("4")),
			eq(new BigDecimal("189.30")),
			eq("USD"),
			any()
		)).thenReturn(new WalletHolding(
			"user",
			"AAPL",
			"Apple Inc.",
			new BigDecimal("4"),
			new BigDecimal("189.30"),
			"USD",
			Instant.EPOCH,
			Instant.EPOCH
		));

		WalletHolding holding = service.saveHolding(" Apple Inc. ", " aapl ", new BigDecimal("4.0"), new BigDecimal("189.30"), "usd");

		assertThat(holding.symbol()).isEqualTo("AAPL");
		assertThat(holding.currencyCode()).isEqualTo("USD");
		verify(repository).save(
			eq("user"),
			eq("AAPL"),
			eq("Apple Inc."),
			eq(new BigDecimal("4")),
			eq(new BigDecimal("189.30")),
			eq("USD"),
			any()
		);
	}

	@Test
	void saveHoldingRejectsFractionalQuantity() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WalletService service = new WalletService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");
		when(tickerLookupTool.canonicalizeSymbol("AAPL")).thenReturn("AAPL");

		assertThatThrownBy(() -> service.saveHolding("Apple Inc.", "AAPL", new BigDecimal("4.25"), new BigDecimal("189.30"), "USD"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("quantity must be a whole number");
	}

	@Test
	void removeHoldingUsesCurrentSessionId() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WalletService service = new WalletService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");
		when(tickerLookupTool.canonicalizeSymbol("AAPL")).thenReturn("AAPL");
		when(repository.deleteByOwnerIdAndSymbol("user", "AAPL")).thenReturn(true);

		boolean removed = service.removeHolding("AAPL");

		assertThat(removed).isTrue();
		verify(repository).deleteByOwnerIdAndSymbol("user", "AAPL");
	}

	@Test
	void saveHoldingRejectsBlankCompanyName() {
		WalletRepository repository = mock(WalletRepository.class);
		TickerLookupTool tickerLookupTool = mock(TickerLookupTool.class);
		AuthenticatedUserContext authenticatedUserContext = mock(AuthenticatedUserContext.class);
		WalletService service = new WalletService(repository, tickerLookupTool, authenticatedUserContext);
		when(authenticatedUserContext.requireCurrentUsername()).thenReturn("user");

		assertThatThrownBy(() -> service.saveHolding(" ", "AAPL", BigDecimal.ONE, new BigDecimal("189.30"), "USD"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("companyName must not be blank");
	}
}
