package com.silvionetto.finance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAppUserRepository implements AppUserRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcAppUserRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<AppUser> findByUsername(String username) {
		List<AppUser> users = this.jdbcTemplate.query(
			"""
				SELECT id, username, password_hash, role, locked, created_at, updated_at
				FROM app_users
				WHERE username = ?
				""",
			this::mapRow,
			username
		);
		return users.stream().findFirst();
	}

	@Override
	public Optional<AppUser> findByGoogleSubject(String subject) {
		List<AppUser> users = this.jdbcTemplate.query(
			"""
				SELECT users.id, users.username, users.password_hash, users.role, users.locked,
					users.created_at, users.updated_at
				FROM app_users users
				JOIN app_user_oauth_identities identities ON identities.user_id = users.id
				WHERE identities.provider = 'google' AND identities.subject = ?
				""",
			this::mapRow,
			subject
		);
		return users.stream().findFirst();
	}

	@Override
	public List<AppUser> findAll() {
		return this.jdbcTemplate.query(
			"""
				SELECT id, username, password_hash, role, locked, created_at, updated_at
				FROM app_users
				ORDER BY username
				""",
			this::mapRow
		);
	}

	@Override
	public AppUser createGoogleUser(String username, String passwordHash, String role, String subject, String email) {
		AppUser user = this.jdbcTemplate.queryForObject(
			"""
				INSERT INTO app_users (username, password_hash, role, locked)
				VALUES (?, ?, ?, FALSE)
				RETURNING id, username, password_hash, role, locked, created_at, updated_at
				""",
			this::mapRow,
			username,
			passwordHash,
			role
		);
		if (user == null) {
			throw new IllegalStateException("Google user account could not be created");
		}
		this.jdbcTemplate.update(
			"""
				INSERT INTO app_user_oauth_identities (user_id, provider, subject, email)
				VALUES (?, 'google', ?, ?)
				""",
			user.id(),
			subject,
			email
		);
		return user;
	}

	@Override
	public long countByRole(String role) {
		Long count = this.jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM app_users WHERE role = ?",
			Long.class,
			role
		);
		return count == null ? 0L : count;
	}

	@Override
	public void deleteByUsername(String username) {
		this.jdbcTemplate.update("DELETE FROM app_users WHERE username = ?", username);
	}

	@Override
	public void updateLocked(String username, boolean locked) {
		this.jdbcTemplate.update(
			"UPDATE app_users SET locked = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?",
			locked,
			username
		);
	}

	private AppUser mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new AppUser(
			rs.getLong("id"),
			rs.getString("username"),
			rs.getString("password_hash"),
			rs.getString("role"),
			rs.getBoolean("locked"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant()
		);
	}
}
