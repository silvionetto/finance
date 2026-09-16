package com.silvionetto.finance;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

	@GetMapping("/account/password")
	public String passwordPage() {
		return "account-password";
	}
}
