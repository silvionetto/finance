# Copilot instructions for this repository

## Project shape
- Spring Boot application in `com.silvionetto.finance`.
- Java 25 toolchain, Gradle build, Spring Boot 4.1.1-SNAPSHOT.
- Current dependencies are Web MVC, Actuator, and Spring AI OpenAI starter.
- `spring.application.name` is set in `src/main/resources/application.properties`.

## Build, test, and run
- Build: `./gradlew build` on macOS/Linux, `gradlew.bat build` on Windows.
- Test: `./gradlew test` or `gradlew.bat test`.
- Single test class: `./gradlew test --tests com.silvionetto.finance.FinanceApplicationTests`.
- Single test method: `./gradlew test --tests com.silvionetto.finance.FinanceApplicationTests.contextLoads`.
- Run the app: `./gradlew bootRun` or `gradlew.bat bootRun`.

## Architecture
- This is a minimal Spring Boot entrypoint today: one `@SpringBootApplication` class and one `@SpringBootTest` context-load test.
- The build is already wired for web/MVC and actuator endpoints, so new features should follow the Spring Boot servlet stack rather than reactive WebFlux.
- Spring AI is included through the OpenAI model starter and managed by the Spring AI BOM in `build.gradle`.

## Conventions
- Keep the main application class in `com.silvionetto.finance`; this is the package root for component scanning.
- Prefer Gradle toolchain settings already declared in `build.gradle` instead of hardcoding a local JDK version.
- Keep tests under `src/test/java` and use JUnit 5 with `@SpringBootTest` for application-context checks.
- When adding Spring AI code, align versions with the existing `springAiVersion` property and BOM import.
- Keep repository-specific configuration in `src/main/resources/application.properties` unless a new profile or config source is required.
