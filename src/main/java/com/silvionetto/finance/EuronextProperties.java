package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "euronext")
public record EuronextProperties(String authKey, String baseUrl) {
}
