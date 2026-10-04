package com.silvionetto.finance;

public record CompanyProfile(
	String companyName,
	String exchange,
	String description,
	String sector,
	String industry,
	String country,
	String website
) {
	public boolean hasCompanyName() {
		return this.companyName != null && !this.companyName.isBlank();
	}

	public boolean hasDescription() {
		return this.description != null && !this.description.isBlank();
	}
}
