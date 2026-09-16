package com.silvionetto.finance;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

	public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void seedBootstrapUsers(BootstrapUsersProperties properties) {
		seedBootstrapUserIfMissing(properties.adminUsername(), properties.adminPassword(), "ADMIN");
		seedBootstrapUserIfMissing(properties.userUsername(), properties.userPassword(), "USER");
	}

	@PreAuthorize("hasRole('ADMIN')")
	public List<AppUser> listUsers() {
		return this.appUserRepository.findAll();
	}

	public void changePassword(String currentPassword, String newPassword) {
		String normalizedCurrentPassword = requirePassword(currentPassword, "currentPassword");
		String normalizedNewPassword = requirePassword(newPassword, "newPassword");
		AppUser currentUser = currentUser();
		if (!this.passwordEncoder.matches(normalizedCurrentPassword, currentUser.passwordHash())) {
			throw new AccessDeniedException("Current password is incorrect");
		}
		this.appUserRepository.updatePassword(currentUser.username(), this.passwordEncoder.encode(normalizedNewPassword));
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

	private void saveBootstrapUser(String username, String password, String role) {
		this.appUserRepository.save(username, this.passwordEncoder.encode(password), role, false);
	}

	private void seedBootstrapUserIfMissing(String username, String password, String role) {
		requireUsername(username);
		String normalizedUsername = username.trim();
		if (!this.appUserRepository.existsByUsername(normalizedUsername)) {
			saveBootstrapUser(normalizedUsername, requirePassword(password, role.toLowerCase() + "Password"), role);
		}
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

	private static String requirePassword(String password, String fieldName) {
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return password.trim();
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
