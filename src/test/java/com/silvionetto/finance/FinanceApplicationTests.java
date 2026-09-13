package com.silvionetto.finance;

import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Import(FinanceApplicationTests.TestConfig.class)
class FinanceApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}
	}

}
