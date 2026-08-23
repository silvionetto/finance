package com.silvionetto.finance;

public record ApiErrorResponse(
	String message,
	String details
) {
}
