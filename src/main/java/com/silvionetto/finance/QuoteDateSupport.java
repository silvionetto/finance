package com.silvionetto.finance;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

final class QuoteDateSupport {

	private QuoteDateSupport() {
	}

	static LocalDate parseDate(Object value, ZoneId zoneId) {
		if (value == null) {
			return null;
		}
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Instant instant) {
			return instant.atZone(zoneId).toLocalDate();
		}
		if (value instanceof Number number) {
			return fromEpoch(number.longValue(), zoneId);
		}

		String text = value.toString().trim();
		if (text.isBlank()) {
			return null;
		}
		if (text.matches("^-?\\d+$")) {
			try {
				return fromEpoch(Long.parseLong(text), zoneId);
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		try {
			return LocalDate.parse(text);
		} catch (DateTimeParseException ignored) {
		}
		try {
			return Instant.parse(text).atZone(zoneId).toLocalDate();
		} catch (DateTimeParseException ignored) {
		}
		try {
			return OffsetDateTime.parse(text).toInstant().atZone(zoneId).toLocalDate();
		} catch (DateTimeParseException ignored) {
		}
		return null;
	}

	private static LocalDate fromEpoch(long rawValue, ZoneId zoneId) {
		long absoluteValue = Math.abs(rawValue);
		if (absoluteValue >= 1_000_000_000_000_000_000L) {
			return Instant.ofEpochMilli(rawValue / 1_000_000).atZone(zoneId).toLocalDate();
		}
		if (absoluteValue >= 1_000_000_000_000_000L) {
			return Instant.ofEpochMilli(rawValue / 1_000).atZone(zoneId).toLocalDate();
		}
		if (absoluteValue >= 1_000_000_000_000L) {
			return Instant.ofEpochMilli(rawValue).atZone(zoneId).toLocalDate();
		}
		return Instant.ofEpochSecond(rawValue).atZone(zoneId).toLocalDate();
	}
}
