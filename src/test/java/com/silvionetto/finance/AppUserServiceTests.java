package com.silvionetto.finance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AppUserServiceTests {

	@Test
	void listUsersReturnsRepositoryUsers() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUserService service = new AppUserService(repository);
		when(repository.findAll()).thenReturn(List.of(new AppUser(1L, "admin", "encoded", "ADMIN", false, Instant.EPOCH, Instant.EPOCH)));

		service.listUsers();

		verify(repository).findAll();
	}

	@Test
	void lockUserRejectsSelfLock() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUserService service = new AppUserService(repository);
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
