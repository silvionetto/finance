package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "polygon")
public record PolygonProperties(String apiKey, String baseUrl) {
}
