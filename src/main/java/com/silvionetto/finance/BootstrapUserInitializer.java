package com.silvionetto.finance;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap-users.enabled", havingValue = "true", matchIfMissing = true)
public class BootstrapUserInitializer implements ApplicationRunner {

	private final AppUserService appUserService;
	private final BootstrapUsersProperties bootstrapUsersProperties;
	private final Environment environment;

	public BootstrapUserInitializer(
		AppUserService appUserService,
		BootstrapUsersProperties bootstrapUsersProperties,
		Environment environment
	) {
		this.appUserService = appUserService;
		this.bootstrapUsersProperties = bootstrapUsersProperties;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		validate(this.environment, this.bootstrapUsersProperties);
		this.appUserService.seedBootstrapUsers(this.bootstrapUsersProperties);
	}

	private static void validate(Environment environment, BootstrapUsersProperties properties) {
		requireValue(properties.adminUsername(), "finance.bootstrap.admin-username");
		requireConfiguredPassword(environment, "finance.bootstrap.admin-password");
		requireValue(properties.userUsername(), "finance.bootstrap.user-username");
		requireConfiguredPassword(environment, "finance.bootstrap.user-password");
	}

	private static String requireValue(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(propertyName + " must not be blank");
		}
		return value.trim();
	}

	private static String requireConfiguredPassword(Environment environment, String propertyName) {
		return requireValue(environment.getProperty(propertyName), propertyName);
	}
}
