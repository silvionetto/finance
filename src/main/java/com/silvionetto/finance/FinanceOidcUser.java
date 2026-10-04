package com.silvionetto.finance;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

final class FinanceOidcUser implements OidcUser {

	private final OidcUser delegate;
	private final String username;
	private final Collection<GrantedAuthority> authorities;

	FinanceOidcUser(OidcUser delegate, String username, GrantedAuthority applicationRole) {
		this.delegate = delegate;
		this.username = username;
		Collection<GrantedAuthority> mergedAuthorities = new ArrayList<>(delegate.getAuthorities());
		mergedAuthorities.add(applicationRole);
		this.authorities = List.copyOf(mergedAuthorities);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.delegate.getAttributes();
	}

	@Override
	public String getName() {
		return this.username;
	}

	@Override
	public Map<String, Object> getClaims() {
		return this.delegate.getClaims();
	}

	@Override
	public OidcIdToken getIdToken() {
		return this.delegate.getIdToken();
	}

	@Override
	public OidcUserInfo getUserInfo() {
		return this.delegate.getUserInfo();
	}
}
