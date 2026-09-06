package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alpaca")
public record AlpacaProperties(
	String apiKeyId,
	String apiSecretKey,
	String baseUrl,
	String feed
) {
}
