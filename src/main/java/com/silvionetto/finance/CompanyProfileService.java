package com.silvionetto.finance;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CompanyProfileService {

	private final RestClient restClient;
	private final FinancialModelingPrepProperties properties;
	private final BrapiMarketDataTool brapiMarketDataTool;

	public CompanyProfileService(
		RestClient.Builder restClientBuilder,
		FinancialModelingPrepProperties properties,
		BrapiMarketDataTool brapiMarketDataTool
	) {
		this.properties = properties;
		this.brapiMarketDataTool = brapiMarketDataTool;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	public Optional<CompanyProfile> findProfile(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}

		if (this.brapiMarketDataTool.isConfigured() && this.brapiMarketDataTool.supportsSymbol(symbol)) {
			Optional<CompanyProfile> profile = this.brapiMarketDataTool.findCompanyProfile(symbol);
			if (profile.isPresent()) {
				return profile;
			}
		}

		if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
			return Optional.empty();
		}

		List<Map<String, Object>> profiles = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/stable/profile")
				.queryParam("symbol", symbol.trim().toUpperCase(Locale.ROOT))
				.queryParam("apikey", this.properties.apiKey())
				.build())
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});

		if (profiles == null || profiles.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(mapProfile(profiles.getFirst()));
	}

	private static CompanyProfile mapProfile(Map<String, Object> profile) {
		return new CompanyProfile(
			text(profile.get("companyName")),
			firstNonBlank(text(profile.get("exchangeShortName")), text(profile.get("exchange"))),
			text(profile.get("description")),
			text(profile.get("sector")),
			text(profile.get("industry")),
			text(profile.get("country")),
			text(profile.get("website"))
		);
	}

	private static String firstNonBlank(String first, String second) {
		return first == null || first.isBlank() ? second : first;
	}

	private static String text(Object value) {
		if (value == null) {
			return null;
		}
		String text = value.toString().trim();
		return text.isEmpty() ? null : text;
	}
}
