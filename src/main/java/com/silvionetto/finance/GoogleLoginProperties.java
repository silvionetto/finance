package com.silvionetto.finance;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finance.google")
public record GoogleLoginProperties(List<String> adminEmails) {

	public GoogleLoginProperties {
		adminEmails = adminEmails == null
			? List.of()
			: adminEmails.stream()
				.filter(email -> email != null && !email.isBlank())
				.map(GoogleLoginProperties::normalizeEmail)
				.distinct()
				.toList();
	}

	public boolean isAdminEmail(String email) {
		return email != null && this.adminEmails.contains(normalizeEmail(email));
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
