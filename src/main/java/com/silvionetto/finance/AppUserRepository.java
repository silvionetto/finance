package com.silvionetto.finance;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository {
	Optional<AppUser> findByUsername(String username);
	List<AppUser> findAll();
	AppUser save(String username, String passwordHash, String role, boolean locked);
	AppUser updatePassword(String username, String passwordHash);
	boolean existsByUsername(String username);
	long countByRole(String role);
	void deleteByUsername(String username);
	void updateLocked(String username, boolean locked);
}
