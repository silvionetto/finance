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
}
