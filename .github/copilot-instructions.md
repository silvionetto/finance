# Copilot instructions for `finance`

## Project shape

- Spring Boot 4.1.1-SNAPSHOT application in `com.silvionetto.finance`.
- Servlet stack only: Spring MVC, Thymeleaf, Actuator, JDBC, Flyway, and Spring AI OpenAI.
- Main user flows are a chat UI at `/`, a JSON chat API at `/api/chat`, and a watchlist API at `/api/watchlist`.

## Build, test, and verify

- Build: `gradlew.bat build` on Windows, `./gradlew build` on macOS/Linux.
- Test: `gradlew.bat test` or `./gradlew test`.
- Single test class: `gradlew.bat test --tests com.silvionetto.finance.ChatControllerTests`
- Single test method: `gradlew.bat test --tests com.silvionetto.finance.FinanceApplicationTests.contextLoads`
- Secret scan: `gradlew.bat scanSecrets`
- Start local PostgreSQL for full startup locally: `docker compose up -d`

## High-level architecture

- `FinanceApplication` is the package root for component scanning and `@ConfigurationPropertiesScan`.
- `ChatPageController` renders the Thymeleaf chat page, `ChatController` handles the JSON chat API, and `WatchlistController` exposes watchlist CRUD.
- `ChatService` builds a `ChatClient` from the injected builder, sends the full conversation history with `.messages(...)`, and attaches AI tools for time, ticker lookup, market data, and watchlist actions.
- `InMemoryChatMemory` is a singleton, synchronized FIFO buffer with a default limit of 100 messages.
- `WatchlistService` resolves ticker symbols, normalizes them to uppercase, and uses the fixed owner id `default`.
- `TickerLookupTool` resolves company names in this order: local company ticker catalog, Polygon lookup if available, then Financial Modeling Prep.
- `PolygonMarketDataTool` and `PolygonTickerLookupClient` call Polygon.io directly with `RestClient`.
- `CompanyTickerCatalog` and `JdbcWatchlistRepository` use JDBC with explicit SQL and `ON CONFLICT` upserts for persistence.
- `FlywayConfig` applies migrations from `src/main/resources/db/migration` during startup.
- `application.properties` externalizes AI, datasource, FMP, and Polygon settings through environment variables.

## Key conventions

- Keep new code under `com.silvionetto.finance` unless there is a strong reason to introduce another package root.
- Prefer records for small DTOs and `@ConfigurationProperties` classes.
- For Spring AI, use `.messages(List<Message>)` when conversation history matters; do not switch to `.user(...)` for multi-turn chat.
- Use `Message.getText()` for AI message content.
- Register each bean only once; avoid mixing `@Component` and `@Bean` for the same type.
- Validate blank input explicitly and fail fast with clear `IllegalArgumentException` or `IllegalStateException` messages.
- Normalize stock symbols to uppercase before persistence or lookup.
- Keep database migrations in `src/main/resources/db/migration` and repository-specific config in `src/main/resources/application.properties`.
- Use `MockRestServiceServer` for `RestClient`-based clients and Mockito plus standalone `MockMvc` for focused controller tests.

## Spring AI implementation notes

- Tool methods are annotated with `@Tool` on Spring-managed components.
- The existing chat flow stores user input before the AI call and assistant output after the call.
- When adding or changing tools, preserve the current pattern of small, deterministic tool methods with explicit validation and no silent fallback behavior.

