package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "financial-modeling-prep")
public record FinancialModelingPrepProperties(String apiKey, String baseUrl) {
}
