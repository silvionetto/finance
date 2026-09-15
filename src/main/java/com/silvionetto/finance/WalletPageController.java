package com.silvionetto.finance;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WalletPageController {

	@GetMapping("/wallet")
	public String walletPage() {
		return "wallet";
	}
}
