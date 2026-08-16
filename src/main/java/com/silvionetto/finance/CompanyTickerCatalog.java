package com.silvionetto.finance;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class CompanyTickerCatalog {

	private final TickerCatalogProperties properties;

	public CompanyTickerCatalog(TickerCatalogProperties properties) {
		this.properties = properties;
	}

	public List<CompanyTickerEntry> load() {
		String resourcePath = this.properties.resourcePath();
		try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
			return new com.fasterxml.jackson.databind.ObjectMapper().readValue(inputStream, new com.fasterxml.jackson.core.type.TypeReference<>() {});
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load ticker catalog from " + resourcePath, ex);
		}
	}

	public void save(List<CompanyTickerEntry> entries) {
		String resourcePath = this.properties.resourcePath();
		try {
			Path path = Paths.get(resourcePath);
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), entries);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to save ticker catalog to " + resourcePath, ex);
		}
	}

	public record CompanyTickerEntry(String company_name, String ticker_symbol, String exchange) {
	}
}
