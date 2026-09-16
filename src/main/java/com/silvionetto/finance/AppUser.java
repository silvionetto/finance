package com.silvionetto.finance;

import java.time.Instant;

public record AppUser(
	Long id,
	String username,
	String passwordHash,
	String role,
	boolean locked,
	Instant createdAt,
	Instant updatedAt
) {
}
