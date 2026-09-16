package com.silvionetto.finance;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AppUserServiceTests {

	@Test
	void seedBootstrapUsersCreatesAdminAndUser() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		AppUserService service = new AppUserService(repository, passwordEncoder);
		BootstrapUsersProperties properties = new BootstrapUsersProperties("admin", "admin-secret", "user", "user-secret");

		when(repository.existsByUsername("admin")).thenReturn(false);
		when(repository.existsByUsername("user")).thenReturn(false);
		when(repository.save(eq("admin"), org.mockito.ArgumentMatchers.anyString(), eq("ADMIN"), eq(false)))
			.thenReturn(new AppUser(1L, "admin", "encoded", "ADMIN", false, Instant.EPOCH, Instant.EPOCH));
		when(repository.save(eq("user"), org.mockito.ArgumentMatchers.anyString(), eq("USER"), eq(false)))
			.thenReturn(new AppUser(2L, "user", "encoded", "USER", false, Instant.EPOCH, Instant.EPOCH));

		service.seedBootstrapUsers(properties);

		verify(repository).save(eq("admin"), org.mockito.ArgumentMatchers.anyString(), eq("ADMIN"), eq(false));
		verify(repository).save(eq("user"), org.mockito.ArgumentMatchers.anyString(), eq("USER"), eq(false));
	}

	@Test
	void seedBootstrapUsersDoesNotOverwriteExistingUsers() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		AppUserService service = new AppUserService(repository, passwordEncoder);
		BootstrapUsersProperties properties = new BootstrapUsersProperties("admin", "admin-secret", "user", "user-secret");
		when(repository.existsByUsername("admin")).thenReturn(true);
		when(repository.existsByUsername("user")).thenReturn(true);

		service.seedBootstrapUsers(properties);

		verify(repository, never()).save(eq("admin"), org.mockito.ArgumentMatchers.anyString(), eq("ADMIN"), eq(false));
		verify(repository, never()).save(eq("user"), org.mockito.ArgumentMatchers.anyString(), eq("USER"), eq(false));
	}

	@Test
	void listUsersReturnsRepositoryUsers() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		AppUserService service = new AppUserService(repository, passwordEncoder);
		when(repository.findAll()).thenReturn(List.of(new AppUser(1L, "admin", "encoded", "ADMIN", false, Instant.EPOCH, Instant.EPOCH)));

		service.listUsers();

		verify(repository).findAll();
	}

	@Test
	void lockUserRejectsSelfLock() {
		AppUserRepository repository = mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		AppUserService service = new AppUserService(repository, passwordEncoder);
		when(repository.findByUsername("admin")).thenReturn(Optional.of(new AppUser(1L, "admin", "hash", "ADMIN", false, Instant.EPOCH, Instant.EPOCH)));
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a"));

		try {
			org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.lockUser("admin"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("You cannot lock your own user");
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
