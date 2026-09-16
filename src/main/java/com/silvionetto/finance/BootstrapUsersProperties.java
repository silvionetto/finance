package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties("finance.bootstrap")
public record BootstrapUsersProperties(
	String adminUsername,
	String adminPassword,
	String userUsername,
	String userPassword
) {

	@ConstructorBinding
	public BootstrapUsersProperties(String adminUsername, String adminPassword, String userUsername, String userPassword) {
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
		this.userUsername = userUsername;
		this.userPassword = userPassword;
	}

	public BootstrapUsersProperties() {
		this("admin", null, "user", null);
	}
}
