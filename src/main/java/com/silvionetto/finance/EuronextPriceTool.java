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
import java.util.Objects;
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
		EuronextSnapshot snapshot = fetchSnapshot(instrument);
		return "instrument=%s, exchange=%s, symbol=%s, productData=%s, price=%s, currency=%s, quoteTimestamp=%s, open=%s, previousClose=%s, tradedQty=%s, trades=%s, vwap=%s, status=%s, summary=%s"
			.formatted(
				firstNonBlank(snapshot.instrumentName(), "n/a"),
				firstNonBlank(snapshot.exchangeLabel(), "n/a"),
				firstNonBlank(snapshot.symbol(), "n/a"),
				firstNonBlank(snapshot.productData(), "n/a"),
				snapshot.price(),
				firstNonBlank(snapshot.currency(), "n/a"),
				firstNonBlank(snapshot.quoteTimestamp(), "n/a"),
				firstNonBlank(snapshot.openPrice(), "n/a"),
				firstNonBlank(snapshot.previousClose(), "n/a"),
				firstNonBlank(snapshot.tradedQuantity(), "n/a"),
				firstNonBlank(snapshot.tradeCount(), "n/a"),
				firstNonBlank(snapshot.vwap(), "n/a"),
				firstNonBlank(snapshot.status(), "n/a"),
				snapshot.summary()
			);
	}

	public boolean isConfigured() {
		return this.properties != null && this.properties.authKey() != null && !this.properties.authKey().isBlank();
	}

	public boolean supportsInstrument(String instrument) {
		if (!isConfigured() || instrument == null || instrument.isBlank()) {
			return false;
		}
		try {
			resolveRequest(instrument);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public String canonicalizeSymbol(String instrument) {
		if (instrument == null || instrument.isBlank()) {
			throw new IllegalArgumentException("instrument must not be blank");
		}
		EuronextRequest request = resolveRequest(instrument);
		if (!"MNE".equals(request.codification())) {
			return request.code() + "-" + request.mic();
		}
		String suffix = micToSuffix(request.mic());
		return suffix == null ? request.code() : request.code() + "." + suffix;
	}

	public StockQuote fetchQuote(String instrument) {
		EuronextSnapshot snapshot = fetchSnapshot(instrument);
		BigDecimal price = toBigDecimal(snapshot.price());
		BigDecimal previousClose = toBigDecimal(snapshot.previousClose());
		BigDecimal change = null;
		BigDecimal changePercent = null;
		if (price != null && previousClose != null) {
			change = price.subtract(previousClose);
			if (previousClose.compareTo(BigDecimal.ZERO) > 0) {
				changePercent = change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 10, java.math.RoundingMode.HALF_UP);
			}
		}
		return new StockQuote(snapshot.symbol(), price, change, changePercent, snapshot.currency());
	}

	private EuronextSnapshot fetchSnapshot(String instrument) {
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
		String productData = firstNonBlank(
			isin != null && mic != null ? isin + "-" + mic : null,
			request.code() + "-" + request.mic()
		);
		String symbol = firstNonBlank(
			normalizeSymbol(findTranscoCode(instrumentMap.get("transco"), "MNE"), mic),
			request.codification().equals("MNE") ? normalizeSymbol(request.code(), request.mic()) : null,
			productData
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
		return new EuronextSnapshot(
			firstNonBlank(toText(instrumentMap.get("longNm")), toText(instrumentMap.get("shrtNm"))),
			firstNonBlank(findExchangeLabel(response.get("exchange"), mic), mic),
			firstNonBlank(symbol, canonicalizeSymbol(instrument)),
			productData,
			price,
			toText(instrumentMap.get("currency")),
			formatDateTime(currentSession.get("lastUpdate")),
			toText(currentSession.get("openPx")),
			toText(currentSession.get("prevAdjClosingPrice")),
			toText(currentSession.get("tradedQty")),
			toText(currentSession.get("nbTrades")),
			toText(currentSession.get("vwap")),
			toText(currentSession.get("instrTradingStatus")),
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

	private static String normalizeSymbol(String symbol, String mic) {
		if (symbol == null || symbol.isBlank()) {
			return null;
		}
		String suffix = micToSuffix(mic);
		String normalized = symbol.trim().toUpperCase(Locale.ROOT);
		return suffix == null ? normalized : normalized + "." + suffix;
	}

	private static String micToSuffix(String mic) {
		return EXCHANGE_SUFFIX_TO_MIC.entrySet().stream()
			.filter(entry -> Objects.equals(entry.getValue(), mic))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
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

	private static BigDecimal toBigDecimal(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return new BigDecimal(value);
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

	private record EuronextSnapshot(
		String instrumentName,
		String exchangeLabel,
		String symbol,
		String productData,
		String price,
		String currency,
		String quoteTimestamp,
		String openPrice,
		String previousClose,
		String tradedQuantity,
		String tradeCount,
		String vwap,
		String status,
		String summary
	) {
	}
}
