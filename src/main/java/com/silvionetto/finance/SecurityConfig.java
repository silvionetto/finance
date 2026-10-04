package com.silvionetto.finance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final GoogleOidcUserService googleOidcUserService;

	public SecurityConfig(GoogleOidcUserService googleOidcUserService) {
		this.googleOidcUserService = googleOidcUserService;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/login", "/error", "/webjars/**", "/oauth2/**", "/login/oauth2/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.loginPage("/login")
				.defaultSuccessUrl("/", true)
				.userInfoEndpoint(userInfo -> userInfo.oidcUserService(this.googleOidcUserService))
			)
			.logout(logout -> logout.logoutSuccessUrl("/login?logout"));
		return http.build();
	}

}
