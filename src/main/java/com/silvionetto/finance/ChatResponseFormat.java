package com.silvionetto.finance;

public enum ChatResponseFormat {

	MARKDOWN("markdown"),
	HTML("html"),
	TEXT("text");

	private final String value;

	ChatResponseFormat(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}
}
