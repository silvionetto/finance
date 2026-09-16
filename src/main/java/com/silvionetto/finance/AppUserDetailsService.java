package com.silvionetto.finance;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public AppUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = this.appUserRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return new User(
			user.username(),
			user.passwordHash(),
			true,
			true,
			true,
			!user.locked(),
			AuthorityUtils.createAuthorityList("ROLE_" + user.role())
		);
	}
}
