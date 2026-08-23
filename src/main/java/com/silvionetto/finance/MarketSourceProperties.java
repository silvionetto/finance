package com.silvionetto.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finance.market")
public record MarketSourceProperties(
	String primarySource,
	String fallbackSource
) {
}
