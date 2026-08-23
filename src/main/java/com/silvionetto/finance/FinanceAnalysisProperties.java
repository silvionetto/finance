package com.silvionetto.finance;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finance.analysis")
public record FinanceAnalysisProperties(
	BigDecimal buyThreshold,
	BigDecimal sellThreshold,
	int refreshMinutes
) {
}
