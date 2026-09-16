package com.silvionetto.finance;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountApiController {

	private final AppUserService appUserService;

	public AccountApiController(AppUserService appUserService) {
		this.appUserService = appUserService;
	}

	@PostMapping("/password")
	public void changePassword(@RequestBody ChangePasswordRequest request) {
		this.appUserService.changePassword(request.currentPassword(), request.newPassword());
	}

	public record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
