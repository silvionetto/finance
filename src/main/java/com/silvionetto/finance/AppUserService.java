package com.silvionetto.finance;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

	private final AppUserRepository appUserRepository;

	public AppUserService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@PreAuthorize("hasRole('ADMIN')")
	public List<AppUser> listUsers() {
		return this.appUserRepository.findAll();
	}

	@PreAuthorize("hasRole('ADMIN')")
	public void deleteUser(String username) {
		AppUser user = requireManagedUser(username);
		guardAgainstSelfManagement(user.username(), "delete your own user");
		guardLastAdmin(user);
		this.appUserRepository.deleteByUsername(user.username());
	}

	@PreAuthorize("hasRole('ADMIN')")
	public void lockUser(String username) {
		updateLocked(username, true);
	}

	@PreAuthorize("hasRole('ADMIN')")
	public void unlockUser(String username) {
		updateLocked(username, false);
	}

	private AppUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalStateException("No authenticated user is available");
		}
		return this.appUserRepository.findByUsername(authentication.getName())
			.orElseThrow(() -> new IllegalStateException("Authenticated user does not exist: " + authentication.getName()));
	}

	private void updateLocked(String username, boolean locked) {
		AppUser user = requireManagedUser(username);
		if (locked) {
			guardAgainstSelfManagement(user.username(), "lock your own user");
			guardLastAdmin(user);
		}
		this.appUserRepository.updateLocked(user.username(), locked);
	}

	private static void requireUsername(String username) {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("username must not be blank");
		}
	}

	private AppUser requireManagedUser(String username) {
		requireUsername(username);
		return this.appUserRepository.findByUsername(username.trim())
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + username.trim()));
	}

	private void guardAgainstSelfManagement(String username, String action) {
		if (currentUser().username().equals(username)) {
			throw new IllegalArgumentException("You cannot " + action);
		}
	}

	private void guardLastAdmin(AppUser user) {
		if ("ADMIN".equals(user.role()) && this.appUserRepository.countByRole("ADMIN") <= 1) {
			throw new IllegalArgumentException("At least one admin user must remain active");
		}
	}
}
