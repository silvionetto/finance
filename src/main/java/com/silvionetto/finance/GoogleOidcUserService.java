package com.silvionetto.finance;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class GoogleOidcUserService extends OidcUserService {

	private final GoogleAccountProvisioningService googleAccountProvisioningService;

	public GoogleOidcUserService(GoogleAccountProvisioningService googleAccountProvisioningService) {
		this.googleAccountProvisioningService = googleAccountProvisioningService;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) {
		OidcUser googleUser = super.loadUser(userRequest);
		Boolean emailVerified = googleUser.getAttribute("email_verified");
		AppUser appUser = this.googleAccountProvisioningService.resolveGoogleAccount(
			googleUser.getSubject(),
			googleUser.getEmail(),
			Boolean.TRUE.equals(emailVerified)
		);
		return new FinanceOidcUser(
			googleUser,
			appUser.username(),
			new SimpleGrantedAuthority("ROLE_" + appUser.role())
		);
	}
}
