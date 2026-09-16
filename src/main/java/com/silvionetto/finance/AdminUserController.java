package com.silvionetto.finance;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AppUserService appUserService;

	public AdminUserController(AppUserService appUserService) {
		this.appUserService = appUserService;
	}

	@GetMapping
	public List<UserSummary> listUsers() {
		return this.appUserService.listUsers().stream()
			.map(user -> new UserSummary(user.username(), user.role(), user.locked()))
			.toList();
	}

	@DeleteMapping("/{username}")
	public void deleteUser(@PathVariable String username) {
		this.appUserService.deleteUser(username);
	}

	@PostMapping("/{username}/lock")
	public void lockUser(@PathVariable String username) {
		this.appUserService.lockUser(username);
	}

	@PostMapping("/{username}/unlock")
	public void unlockUser(@PathVariable String username) {
		this.appUserService.unlockUser(username);
	}

	public record UserSummary(String username, String role, boolean locked) {}
}
