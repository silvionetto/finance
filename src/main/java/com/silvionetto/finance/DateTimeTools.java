package com.silvionetto.finance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DateTimeTools {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Tool(description = "Get the current date and time")
	public String getCurrentDateTime() {
		return LocalDateTime.now().format(FORMATTER);
	}
}
