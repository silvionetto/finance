package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.dao.DuplicateKeyException;

class GoogleAccountProvisioningServiceTests {

	@Test
	void provisionsTrustedGoogleEmailAsAdmin() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			repository,
			new GoogleLoginProperties(List.of("Admin@Example.com")),
			passwordEncoder
		);
		AppUser admin = user("admin@example.com", "ADMIN", false);
		when(repository.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
		when(repository.findByUsername("admin@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("unusable-password-hash");
		when(repository.createGoogleUser(
			"admin@example.com",
			"unusable-password-hash",
			"ADMIN",
			"google-sub",
			"admin@example.com"
		)).thenReturn(admin);

		AppUser result = service.resolveGoogleAccount("google-sub", "Admin@Example.com", true);

		assertThat(result).isEqualTo(admin);
		verify(repository).createGoogleUser(
			"admin@example.com",
			"unusable-password-hash",
			"ADMIN",
			"google-sub",
			"admin@example.com"
		);
	}

	@Test
	void provisionsOtherGoogleEmailsAsRegularUsers() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			repository,
			new GoogleLoginProperties(List.of()),
			passwordEncoder
		);
		AppUser regularUser = user("user@example.com", "USER", false);
		when(repository.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
		when(repository.findByUsername("user@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("unusable-password-hash");
		when(repository.createGoogleUser(
			"user@example.com",
			"unusable-password-hash",
			"USER",
			"google-sub",
			"user@example.com"
		)).thenReturn(regularUser);

		assertThat(service.resolveGoogleAccount("google-sub", "user@example.com", true).role()).isEqualTo("USER");
	}

	@Test
	void existingGoogleIdentityKeepsPersistedRoleAndDoesNotReprovision() {
		AppUserRepository repository = mock(AppUserRepository.class);
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			repository,
			new GoogleLoginProperties(List.of()),
			mock(PasswordEncoder.class)
		);
		AppUser admin = user("admin@example.com", "ADMIN", false);
		when(repository.findByGoogleSubject("google-sub")).thenReturn(Optional.of(admin));

		assertThat(service.resolveGoogleAccount("google-sub", "admin@example.com", true)).isEqualTo(admin);
		verify(repository, never()).createGoogleUser(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void lockedGoogleIdentityIsRejected() {
		AppUserRepository repository = mock(AppUserRepository.class);
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			repository,
			new GoogleLoginProperties(List.of()),
			mock(PasswordEncoder.class)
		);
		when(repository.findByGoogleSubject("google-sub"))
			.thenReturn(Optional.of(user("user@example.com", "USER", true)));

		assertThatThrownBy(() -> service.resolveGoogleAccount("google-sub", "user@example.com", true))
			.isInstanceOf(DisabledException.class);
	}

	@Test
	void requiresVerifiedEmail() {
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			mock(AppUserRepository.class),
			new GoogleLoginProperties(List.of()),
			mock(PasswordEncoder.class)
		);

		assertThatThrownBy(() -> service.resolveGoogleAccount("google-sub", "user@example.com", false))
			.isInstanceOf(OAuth2AuthenticationException.class);
	}

	@Test
	void doesNotAutomaticallyLinkGoogleIdentityToLegacyUsername() {
		AppUserRepository repository = mock(AppUserRepository.class);
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			repository,
			new GoogleLoginProperties(List.of()),
			mock(PasswordEncoder.class)
		);
		when(repository.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
		when(repository.findByUsername("user@example.com"))
			.thenReturn(Optional.of(user("user@example.com", "USER", false)));

		assertThatThrownBy(() -> service.resolveGoogleAccount("google-sub", "user@example.com", true))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.hasMessageContaining("migrate or link it explicitly");
		verify(repository, never()).createGoogleUser(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void reportsConcurrentAccountCreationConflictAsAuthenticationFailure() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		GoogleAccountProvisioningService service = new GoogleAccountProvisioningService(
			repository,
			new GoogleLoginProperties(List.of()),
			passwordEncoder
		);
		when(repository.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
		when(repository.findByUsername("user@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("unusable-password-hash");
		doThrow(new DuplicateKeyException("unique identity conflict"))
			.when(repository).createGoogleUser(
				"user@example.com",
				"unusable-password-hash",
				"USER",
				"google-sub",
				"user@example.com"
			);

		assertThatThrownBy(() -> service.resolveGoogleAccount("google-sub", "user@example.com", true))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.hasMessageContaining("conflicts with an existing Finance account");
	}

	private static AppUser user(String username, String role, boolean locked) {
		return new AppUser(1L, username, "stored-hash", role, locked, Instant.EPOCH, Instant.EPOCH);
	}
}
