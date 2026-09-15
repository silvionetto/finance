package com.silvionetto.finance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

class WalletPageControllerTests {

	@Test
	void walletPageUsesWalletTemplate() {
		WalletPageController controller = new WalletPageController();

		org.assertj.core.api.Assertions.assertThat(controller.walletPage()).isEqualTo("wallet");
	}

	@Test
	void walletTemplateIncludesWalletFormAndListHooks() throws Exception {
		String template = StreamUtils.copyToString(
			new ClassPathResource("templates/wallet.html").getInputStream(),
			StandardCharsets.UTF_8
		);

		assertThat(template, containsString("id=\"wallet-form\""));
		assertThat(template, containsString("id=\"companyName\""));
		assertThat(template, containsString("id=\"symbol\""));
		assertThat(template, containsString("id=\"quantity\""));
		assertThat(template, containsString("id=\"averageCost\""));
		assertThat(template, containsString("id=\"currencyCode\""));
		assertThat(template, containsString("id=\"wallet-status\""));
		assertThat(template, containsString("id=\"wallet-list\""));
		assertThat(template, containsString("id=\"refresh-wallet\""));
		assertThat(template, containsString("id=\"back-to-chat\""));
		assertThat(template, containsString("function loadWallet()"));
		assertThat(template, containsString("function renderHoldings(holdings)"));
		assertThat(template, containsString("function renderLoadErrorState(message)"));
		assertThat(template, containsString("function updateSummary(holdings)"));
		assertThat(template, containsString("fetch('/api/wallet'"));
		assertThat(template, containsString("Unable to load your wallet right now. Refresh and try again."));
	}
}
