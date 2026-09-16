package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class SecurityConfigTests {

	@Test
	void bootstrapUserIsLoadedWithUserRole() {
		AppUserRepository repository = new StubAppUserRepository(
			Optional.of(new AppUser(1L, "user", "hash", "USER", false, Instant.EPOCH, Instant.EPOCH))
		);
		AppUserDetailsService service = new AppUserDetailsService(repository);

		UserDetails userDetails = service.loadUserByUsername("user");

		assertThat(userDetails.getUsername()).isEqualTo("user");
		assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
		assertThat(userDetails.isAccountNonLocked()).isTrue();
	}

	@Test
	void lockedUserIsLoadedAsLocked() {
		AppUserRepository repository = new StubAppUserRepository(
			Optional.of(new AppUser(1L, "admin", "hash", "ADMIN", true, Instant.EPOCH, Instant.EPOCH))
		);
		AppUserDetailsService service = new AppUserDetailsService(repository);

		UserDetails userDetails = service.loadUserByUsername("admin");

		assertThat(userDetails.isAccountNonLocked()).isFalse();
	}

	private static final class StubAppUserRepository implements AppUserRepository {

		private final Optional<AppUser> user;

		private StubAppUserRepository(Optional<AppUser> user) {
			this.user = user;
		}

		@Override
		public Optional<AppUser> findByUsername(String username) {
			return this.user.filter(candidate -> candidate.username().equals(username));
		}

		@Override
		public List<AppUser> findAll() {
			return this.user.stream().toList();
		}

		@Override
		public AppUser save(String username, String passwordHash, String role, boolean locked) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AppUser updatePassword(String username, String passwordHash) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean existsByUsername(String username) {
			return findByUsername(username).isPresent();
		}

		@Override
		public long countByRole(String role) {
			return this.user.filter(candidate -> candidate.role().equals(role)).isPresent() ? 1L : 0L;
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
