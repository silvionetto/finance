package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AccountControllerTests {

	@Test
	void passwordPageUsesTemplate() {
		assertThat(new AccountController().passwordPage()).isEqualTo("account-password");
	}

	@Test
	void changePasswordDelegatesToService() throws Exception {
		AppUserService appUserService = mock(AppUserService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AccountApiController(appUserService)).build();

		mockMvc.perform(post("/api/account/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "currentPassword": "old-secret",
					  "newPassword": "new-secret"
					}
					"""))
			.andExpect(status().isOk());

		verify(appUserService).changePassword("old-secret", "new-secret");
	}

	@Test
	void changePasswordUpdatesStoredHash() {
		InMemoryAppUserRepository repository = new InMemoryAppUserRepository();
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		AppUserService service = new AppUserService(repository, passwordEncoder);
		repository.save("user", passwordEncoder.encode("old-secret"), "USER", false);
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "n/a"));

		try {
			service.changePassword("old-secret", "new-secret");
		} finally {
			SecurityContextHolder.clearContext();
		}

		AppUser updatedUser = repository.findByUsername("user").orElseThrow();
		assertThat(passwordEncoder.matches("new-secret", updatedUser.passwordHash())).isTrue();
	}

	private static final class InMemoryAppUserRepository implements AppUserRepository {

		private AppUser storedUser;

		@Override
		public Optional<AppUser> findByUsername(String username) {
			return Optional.ofNullable(this.storedUser).filter(user -> user.username().equals(username));
		}

		@Override
		public List<AppUser> findAll() {
			return this.storedUser == null ? List.of() : List.of(this.storedUser);
		}

		@Override
		public AppUser save(String username, String passwordHash, String role, boolean locked) {
			this.storedUser = new AppUser(1L, username, passwordHash, role, locked, Instant.EPOCH, Instant.EPOCH);
			return this.storedUser;
		}

		@Override
		public AppUser updatePassword(String username, String passwordHash) {
			this.storedUser = new AppUser(1L, username, passwordHash, this.storedUser.role(), this.storedUser.locked(), Instant.EPOCH, Instant.EPOCH);
			return this.storedUser;
		}

		@Override
		public boolean existsByUsername(String username) {
			return this.storedUser != null && this.storedUser.username().equals(username);
		}

		@Override
		public long countByRole(String role) {
			return this.storedUser != null && this.storedUser.role().equals(role) ? 1L : 0L;
		}

		@Override
		public void deleteByUsername(String username) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void updateLocked(String username, boolean locked) {
			throw new UnsupportedOperationException();
		}
	}
}
