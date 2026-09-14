package com.silvionetto.finance;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EuronextPriceTool {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE = new ParameterizedTypeReference<>() {
	};
	private static final Pattern PRODUCT_DATA_PATTERN = Pattern.compile("^([A-Z0-9]+)-([A-Z]{4})$");
	private static final Pattern SYMBOL_SUFFIX_PATTERN = Pattern.compile("^([A-Z0-9]+)\\.([A-Z]{2})$");
	private static final Pattern ISIN_PATTERN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}\\d$");
	private static final DateTimeFormatter EURONEXT_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");
	private static final Map<String, String> EXCHANGE_SUFFIX_TO_MIC = Map.of(
		"AS", "XAMS",
		"BR", "XBRU",
		"LS", "XLIS",
		"PA", "XPAR"
	);

	private final RestClient restClient;
	private final EuronextProperties properties;

	public EuronextPriceTool(RestClient.Builder restClientBuilder, EuronextProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Tool(description = "Get the latest Euronext quote, timestamp, and market summary from a Euronext product URL, product data like NL0011540547-XAMS, or symbol like ABN.AS")
	public String getEuronextQuote(String instrument) {
		if (instrument == null || instrument.isBlank()) {
			throw new IllegalArgumentException("instrument must not be blank");
		}
		if (this.properties.authKey() == null || this.properties.authKey().isBlank()) {
			throw new IllegalStateException("Euronext auth key is not configured");
		}

		EuronextRequest request = resolveRequest(instrument);
		Map<String, Object> response = this.restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/api/instrumentDetail")
				.queryParam("code", request.code())
				.queryParam("codification", request.codification())
				.queryParam("exchCode", request.mic())
				.queryParam("sessionQuality", "RT")
				.queryParam("view", "FULL")
				.queryParam("authKey", this.properties.authKey())
				.build())
			.retrieve()
			.body(MAP_RESPONSE);

		if (response == null || response.isEmpty()) {
			throw new IllegalStateException("Euronext returned an empty response for instrument: " + instrument);
		}

		Map<String, Object> instrumentMap = map(response.get("instr"));
		Map<String, Object> currentSession = map(instrumentMap.get("currInstrSess"));
		if (instrumentMap.isEmpty() || currentSession.isEmpty()) {
			throw new IllegalStateException("Euronext quote data is missing for instrument: " + instrument);
		}

		String price = toText(currentSession.get("lastPx"));
		if (price == null) {
			throw new IllegalStateException("Euronext last price is missing for instrument: " + instrument);
		}

		String mic = firstNonBlank(toText(instrumentMap.get("mic")), request.mic());
		String isin = toText(instrumentMap.get("cdStand"));
		String symbol = firstNonBlank(findTranscoCode(instrumentMap.get("transco"), "MNE"), request.codification().equals("MNE") ? request.code() : null);
		String productData = firstNonBlank(
			isin != null && mic != null ? isin + "-" + mic : null,
			request.code() + "-" + request.mic()
		);

		String summary = buildSummary(
			toText(currentSession.get("lastPx")),
			toText(instrumentMap.get("currency")),
			toText(currentSession.get("openPx")),
			toText(currentSession.get("prevAdjClosingPrice")),
			toText(currentSession.get("tradedQty")),
			toText(currentSession.get("nbTrades")),
			toText(currentSession.get("vwap")),
			toText(currentSession.get("instrTradingStatus"))
		);

		return "instrument=%s, exchange=%s, symbol=%s, productData=%s, price=%s, currency=%s, quoteTimestamp=%s, open=%s, previousClose=%s, tradedQty=%s, trades=%s, vwap=%s, status=%s, summary=%s"
			.formatted(
				firstNonBlank(toText(instrumentMap.get("longNm")), toText(instrumentMap.get("shrtNm")), "n/a"),
				firstNonBlank(findExchangeLabel(response.get("exchange"), mic), mic, "n/a"),
				firstNonBlank(symbol, "n/a"),
				firstNonBlank(productData, "n/a"),
				price,
				firstNonBlank(toText(instrumentMap.get("currency")), "n/a"),
				firstNonBlank(formatDateTime(currentSession.get("lastUpdate")), "n/a"),
				firstNonBlank(toText(currentSession.get("openPx")), "n/a"),
				firstNonBlank(toText(currentSession.get("prevAdjClosingPrice")), "n/a"),
				firstNonBlank(toText(currentSession.get("tradedQty")), "n/a"),
				firstNonBlank(toText(currentSession.get("nbTrades")), "n/a"),
				firstNonBlank(toText(currentSession.get("vwap")), "n/a"),
				firstNonBlank(toText(currentSession.get("instrTradingStatus")), "n/a"),
				summary
			);
	}

	private static EuronextRequest resolveRequest(String input) {
		String trimmed = input.trim();
		if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
			return resolveRequestFromUrl(trimmed);
		}

		String normalized = trimmed.toUpperCase(Locale.ROOT);
		Matcher productDataMatcher = PRODUCT_DATA_PATTERN.matcher(normalized);
		if (productDataMatcher.matches()) {
			String code = productDataMatcher.group(1);
			String mic = productDataMatcher.group(2);
			return new EuronextRequest(code, isIsin(code) ? "ISIN" : "MNE", mic);
		}

		Matcher suffixedSymbolMatcher = SYMBOL_SUFFIX_PATTERN.matcher(normalized);
		if (suffixedSymbolMatcher.matches()) {
			String code = suffixedSymbolMatcher.group(1);
			String suffix = suffixedSymbolMatcher.group(2);
			String mic = EXCHANGE_SUFFIX_TO_MIC.get(suffix);
			if (mic == null) {
				throw new IllegalArgumentException("Unsupported Euronext exchange suffix: ." + suffix + ". Supported suffixes: .AS, .BR, .LS, .PA");
			}
			return new EuronextRequest(code, "MNE", mic);
		}

		throw new IllegalArgumentException("instrument must be a Euronext product URL, product data like NL0011540547-XAMS, or symbol like ABN.AS");
	}

	private static EuronextRequest resolveRequestFromUrl(String input) {
		URI uri = URI.create(input);
		String path = uri.getPath();
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("Euronext product URL path must not be blank");
		}
		String[] segments = path.split("/");
		String lastSegment = segments[segments.length - 1];
		if (lastSegment == null || lastSegment.isBlank()) {
			throw new IllegalArgumentException("Could not extract Euronext product data from URL: " + input);
		}
		return resolveRequest(lastSegment);
	}

	private static String buildSummary(
		String lastPrice,
		String currency,
		String openPrice,
		String previousClose,
		String tradedQuantity,
		String trades,
		String vwap,
		String status
	) {
		List<String> parts = Stream.of(
			lastPrice == null ? null : "last price " + lastPrice + (currency == null ? "" : " " + currency),
			openPrice == null ? null : "opened at " + openPrice,
			previousClose == null ? null : "previous close " + previousClose,
			tradedQuantity == null && trades == null ? null : "volume " + firstNonBlank(tradedQuantity, "n/a") + " across " + firstNonBlank(trades, "n/a") + " trades",
			vwap == null ? null : "VWAP " + vwap,
			status == null ? null : "status " + status
		).filter(value -> value != null && !value.isBlank()).toList();
		return parts.isEmpty() ? "No market summary available" : String.join("; ", parts);
	}

	private static boolean isIsin(String code) {
		return ISIN_PATTERN.matcher(code).matches();
	}

	private static String findTranscoCode(Object value, String codification) {
		for (Map<String, Object> entry : listOfMaps(value)) {
			if (codification.equalsIgnoreCase(firstNonBlank(toText(entry.get("codification")), ""))) {
				return toText(entry.get("code"));
			}
		}
		return null;
	}

	private static String findExchangeLabel(Object value, String mic) {
		for (Map<String, Object> entry : listOfMaps(value)) {
			if (mic != null && mic.equalsIgnoreCase(toText(entry.get("exchCd")))) {
				return toText(entry.get("exchlbl"));
			}
		}
		return null;
	}

	private static String formatDateTime(Object value) {
		String text = toText(value);
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			return LocalDateTime.parse(text, EURONEXT_DATE_TIME).toString();
		} catch (DateTimeParseException ex) {
			return text;
		}
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static String toText(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal.stripTrailingZeros().toPlainString();
		}
		if (value instanceof Number number) {
			return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
		}
		String text = value.toString().trim();
		return text.isBlank() ? null : text;
	}

	private static List<Map<String, Object>> listOfMaps(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		return list.stream().map(EuronextPriceTool::map).toList();
	}

	private static Map<String, Object> map(Object value) {
		if (!(value instanceof Map<?, ?> source)) {
			return Map.of();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		source.forEach((key, nestedValue) -> {
			if (key != null) {
				result.put(key.toString(), nestedValue);
			}
		});
		return result;
	}

	private record EuronextRequest(String code, String codification, String mic) {
	}
}
