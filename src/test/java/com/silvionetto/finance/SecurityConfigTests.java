package com.silvionetto.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class SecurityConfigTests {

	@Test
	void trustedAdminEmailsAreNormalized() {
		GoogleLoginProperties properties = new GoogleLoginProperties(List.of(" Admin@Example.com ", "user@example.com"));

		assertThat(properties.isAdminEmail("admin@example.COM")).isTrue();
		assertThat(properties.isAdminEmail("unknown@example.com")).isFalse();
	}

	@Test
	void oidcPrincipalUsesApplicationUsernameForDataScoping() {
		OidcUser delegate = mock(OidcUser.class);
		doReturn(List.of(new SimpleGrantedAuthority("SCOPE_openid"))).when(delegate).getAuthorities();
		when(delegate.getAttributes()).thenReturn(Map.of("sub", "google-subject", "email", "person@example.com"));
		FinanceOidcUser principal = new FinanceOidcUser(
			delegate,
			"person@example.com",
			new SimpleGrantedAuthority("ROLE_USER")
		);

		assertThat(principal.getName()).isEqualTo("person@example.com");
		String subject = principal.getAttribute("sub");
		assertThat(subject).isEqualTo("google-subject");
		assertThat(principal.getAuthorities())
			.extracting("authority")
			.contains("SCOPE_openid", "ROLE_USER");
	}
}
