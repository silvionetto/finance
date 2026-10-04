package com.silvionetto.finance;

import java.util.UUID;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

@Service
public class GoogleAccountProvisioningService {

	private final AppUserRepository appUserRepository;
	private final GoogleLoginProperties googleLoginProperties;
	private final PasswordEncoder passwordEncoder;

	public GoogleAccountProvisioningService(
		AppUserRepository appUserRepository,
		GoogleLoginProperties googleLoginProperties,
		PasswordEncoder passwordEncoder
	) {
		this.appUserRepository = appUserRepository;
		this.googleLoginProperties = googleLoginProperties;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AppUser resolveGoogleAccount(String subject, String email, boolean emailVerified) {
		if (subject == null || subject.isBlank()) {
			throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_identity"), "Google did not provide a subject identifier");
		}
		if (email == null || email.isBlank() || !emailVerified) {
			throw new OAuth2AuthenticationException(new OAuth2Error("unverified_google_email"), "A verified Google email address is required");
		}

		String normalizedEmail = GoogleLoginProperties.normalizeEmail(email);
		if (normalizedEmail.length() > 320) {
			throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_email"), "Google email address exceeds the supported length");
		}

		AppUser existingAccount = this.appUserRepository.findByGoogleSubject(subject)
			.orElse(null);
		if (existingAccount != null) {
			if (existingAccount.locked()) {
				throw new DisabledException("This Finance account is locked");
			}
			return existingAccount;
		}
		if (this.appUserRepository.findByUsername(normalizedEmail).isPresent()) {
			throw new OAuth2AuthenticationException(
				new OAuth2Error("google_account_not_linked"),
				"This email belongs to an existing local account; an administrator must migrate or link it explicitly"
			);
		}

		String role = this.googleLoginProperties.isAdminEmail(normalizedEmail) ? "ADMIN" : "USER";
		String unusablePasswordHash = this.passwordEncoder.encode(UUID.randomUUID().toString());
		try {
			return this.appUserRepository.createGoogleUser(
				normalizedEmail,
				unusablePasswordHash,
				role,
				subject,
				normalizedEmail
			);
		} catch (DuplicateKeyException exception) {
			throw new OAuth2AuthenticationException(
				new OAuth2Error("google_account_conflict"),
				"Google account conflicts with an existing Finance account; contact an administrator to resolve the account"
			);
		}
	}
}
