package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brapi")
public record BrapiProperties(String apiToken, String baseUrl) {
}
