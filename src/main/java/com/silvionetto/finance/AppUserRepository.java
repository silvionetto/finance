package com.silvionetto.finance;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository {
	Optional<AppUser> findByUsername(String username);
	Optional<AppUser> findByGoogleSubject(String subject);
	List<AppUser> findAll();
	AppUser createGoogleUser(String username, String passwordHash, String role, String subject, String email);
	long countByRole(String role);
	void deleteByUsername(String username);
	void updateLocked(String username, boolean locked);
}
