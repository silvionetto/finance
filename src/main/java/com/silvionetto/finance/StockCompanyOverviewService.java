package com.silvionetto.finance;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class StockCompanyOverviewService {

	private final CompanyTickerCatalog companyTickerCatalog;
	private final CompanyProfileService companyProfileService;

	public StockCompanyOverviewService(
		CompanyTickerCatalog companyTickerCatalog,
		CompanyProfileService companyProfileService
	) {
		this.companyTickerCatalog = companyTickerCatalog;
		this.companyProfileService = companyProfileService;
	}

	public StockCompanyOverview build(WatchlistEntry entry) {
		String symbol = entry.symbol().trim().toUpperCase(Locale.ROOT);
		CompanyTickerCatalog.CompanyTickerEntry catalogEntry = this.companyTickerCatalog.findByTickerSymbol(symbol).orElse(null);
		CompanyProfile profile = this.companyProfileService.findProfile(symbol).orElse(null);
		String savedName = entry.companyName();
		String companyName = isUsefulName(savedName, symbol)
			? savedName.trim()
			: firstNonBlank(catalogEntry == null ? null : catalogEntry.company_name(),
				profile == null ? null : profile.companyName(), symbol);
		String market = firstNonBlank(
			catalogEntry == null ? null : catalogEntry.exchange(),
			profile == null ? null : profile.exchange()
		);

		return new StockCompanyOverview(
			symbol,
			companyName,
			market,
			profile == null ? null : profile.description(),
			profile == null ? null : profile.sector(),
			profile == null ? null : profile.industry(),
			profile == null ? null : profile.country()
		);
	}

	private static boolean isUsefulName(String name, String symbol) {
		return name != null && !name.isBlank() && !name.trim().equalsIgnoreCase(symbol);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}
}
