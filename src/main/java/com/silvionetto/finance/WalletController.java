package com.silvionetto.finance;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

	private final WalletService walletService;
	private final RequestSessionContext requestSessionContext;

	public WalletController(WalletService walletService, RequestSessionContext requestSessionContext) {
		this.walletService = walletService;
		this.requestSessionContext = requestSessionContext;
	}

	@GetMapping
	public List<WalletHoldingResponse> list(HttpSession session) {
		return this.requestSessionContext.withSession(session.getId(), () -> this.walletService.listHoldings().stream()
			.map(WalletHoldingResponse::from)
			.toList());
	}

	@PostMapping
	public WalletHoldingResponse save(HttpSession session, @RequestBody WalletHoldingRequest request) {
		return this.requestSessionContext.withSession(
			session.getId(),
			() -> WalletHoldingResponse.from(this.walletService.saveHolding(
				request.companyName(),
				request.symbol(),
				request.quantity(),
				request.averageCost(),
				request.currencyCode()
			))
		);
	}

	@DeleteMapping
	public void remove(HttpSession session, @RequestBody WalletHoldingDeleteRequest request) {
		this.requestSessionContext.withSession(session.getId(), () -> this.walletService.removeHolding(request.symbol()));
	}

	public record WalletHoldingRequest(
		String companyName,
		String symbol,
		BigDecimal quantity,
		BigDecimal averageCost,
		String currencyCode
	) {}

	public record WalletHoldingDeleteRequest(String symbol) {}

	public record WalletHoldingResponse(
		String symbol,
		String companyName,
		BigDecimal quantity,
		BigDecimal averageCost,
		String currencyCode,
		java.time.Instant createdAt,
		java.time.Instant updatedAt
	) {
		static WalletHoldingResponse from(WalletHolding holding) {
			return new WalletHoldingResponse(
				holding.symbol(),
				holding.companyName(),
				holding.quantity(),
				holding.averageCost(),
				holding.currencyCode(),
				holding.createdAt(),
				holding.updatedAt()
			);
		}
	}
}
