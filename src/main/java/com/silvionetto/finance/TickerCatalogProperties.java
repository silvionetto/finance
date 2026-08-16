package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticker-catalog")
public record TickerCatalogProperties(String resourcePath) {
}
