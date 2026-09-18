# Finance

Spring Boot 4.1 application for a personal AI chat UI backed by Spring AI OpenAI.

## What it does

- Serves a chat page at `/` with a watchlist panel for adding and removing stocks
- Exposes a JSON chat API at `POST /api/chat` with **conversation memory** support
- Maintains chat history within a session for contextual responses
- Uses Thymeleaf for the web UI
- Provides actuator endpoints via Spring Boot Actuator
- Integrates AI tools (e.g., date/time functions, ticker lookup, market prices, Alpaca real-time quotes) for function calling
- Shows the latest watchlist stock-check snapshot with a simple buy / hold / sell recommendation
- Persists immutable, authenticated-user-owned stock analyses and market observations
- Provides watchlist-linked stock detail pages with on-demand comparison data

## Requirements

- Java 25
- Gradle
- An OpenAI-compatible API key in `OPENAI_API_KEY` or `AZURE_OPENAI_API_KEY`
- A Polygon.io API key in `POLYGON_API_KEY` for market price lookups
- An Alpaca Market Data API key pair in `ALPACA_API_KEY_ID` and `ALPACA_API_SECRET_KEY` for real-time quotes
- A local PostgreSQL database with pgvector support for watchlist persistence, ticker catalog storage, and semantic search

## Configuration

`src/main/resources/application.properties` supports:

- `spring.application.name=finance`
- `spring.ai.openai.api-key`
- `spring.ai.openai.base-url`
- `spring.ai.openai.chat.options.model`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.ai.vectorstore.pgvector.initialize-schema`
- `polygon.api-key`
- `polygon.base-url`
- `alpaca.api-key-id`
- `alpaca.api-secret-key`
- `alpaca.base-url`
- `alpaca.feed`

Environment variables can override the AI settings:

- `OPENAI_API_KEY`
- `AZURE_OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `POLYGON_API_KEY`
- `POLYGON_BASE_URL`
- `ALPACA_API_KEY_ID`
- `ALPACA_API_SECRET_KEY`
- `ALPACA_BASE_URL`
- `ALPACA_FEED`

The current defaults are:

- API key: `OPENAI_API_KEY`, falling back to `AZURE_OPENAI_API_KEY`
- Base URL: `https://api.openai.com/v1`
- Chat model: `gpt-4o-mini`
- Database URL: `jdbc:postgresql://localhost:5432/finance`
- Database user/password: `finance` / `finance`

For a local or alternate OpenAI-compatible provider, set `OPENAI_BASE_URL` and `OPENAI_MODEL` to match that service.

### Local PostgreSQL

Start the database with:

```bash
docker compose up -d
```

The compose file uses the `pgvector/pgvector:pg16` image and exposes PostgreSQL on port `5432`.
Spring AI is configured to initialize the vector store schema on startup, and Flyway migrations are executed explicitly from `FlywayConfig` during application startup. If you already had the database running before the watchlist migration was added, restart the app after the new migration file is present so Flyway can apply it.

The Polygon tool currently uses the `v1/open-close/{symbol}/{date}` endpoint to return open and close prices for a trading day. Provide dates in `YYYY-MM-DD` format.

The Alpaca chat tool uses the Market Data API on `data.alpaca.markets` and defaults to the IEX feed. Provide a ticker symbol to get the latest real-time quote.

## Troubleshooting (Frequent Issues)

### App fails to start with `Failed to obtain JDBC Connection` / `Connection to localhost:5432 refused`

If startup fails with errors like:

- `Error creating bean with name 'vectorStore'`
- `Failed to obtain JDBC Connection`
- `Connection to localhost:5432 refused`

the app cannot connect to PostgreSQL used by pgvector.

Fix options:

1. Start local Postgres (recommended for local development):

   ```bash
   docker compose up -d postgres
   ```

2. Point Spring to another running PostgreSQL instance by setting:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`

3. Temporarily run without vector store/Flyway:
   - `SPRING_AI_VECTORSTORE_PGVECTOR_ENABLED=false`
   - `SPRING_FLYWAY_ENABLED=false`

## Run

```bash
gradlew.bat bootRun
```

Then open `http://localhost:8080`.

## How to use the app

### 1. Open the UI

Start the app and open `http://localhost:8080`. The page gives you:

- a chat panel for AI prompts
- a watchlist panel for saving tickers or company names
- the latest stock-check snapshot with recommendations

### 2. Add a stock to the watchlist

Use the watchlist form in the UI, or call the API directly:

```bash
curl.exe -X POST http://localhost:8080/api/watchlist -H "Content-Type: application/json" -d "{\"symbolOrCompanyName\":\"AAPL\"}"
```

You can also use a company name:

```bash
curl.exe -X POST http://localhost:8080/api/watchlist -H "Content-Type: application/json" -d "{\"symbolOrCompanyName\":\"Apple Inc.\"}"
```

### 3. Ask the chat API a question

```bash
curl.exe -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d "{\"prompt\":\"Summarize my watchlist and tell me what is moving today.\"}"
```

The chat keeps conversation memory for the current app session, so follow-up questions can refer to prior messages.

### 4. Check the latest recommendation snapshot

Fetch the current stock-check results:

```bash
curl.exe http://localhost:8080/api/stock-check
```

Refresh the snapshot on demand:

```bash
curl.exe -X POST http://localhost:8080/api/stock-check/refresh
```

The response includes the latest price data, the recommendation, and a short explanation for each watchlist item.

Each stock-check refresh also records the provider observation fields that are available. Open a watchlist symbol at
`/stocks/{symbol}` to review immutable reports, create a new report, or load comparison data on demand. The analysis
API is available at `GET /api/stocks/{symbol}/analyses`, `POST /api/stocks/{symbol}/analyses`, and
`GET /api/stocks/{symbol}/comparison`.

## Build

```bash
gradlew.bat build
```

## Test

```bash
gradlew.bat test
```

## Automated releases

The repository now includes a GitHub Actions workflow at `.github/workflows/release.yml` that runs on every push to `main`.

- `feat:` commits produce a **minor** version bump
- `fix:` commits produce a **patch** version bump
- commits marked with `!` or a `BREAKING CHANGE:` footer produce a **major** version bump
- pushes that contain none of the above commit types do **not** create a release

The workflow uses the latest `v*` git tag as the current release version, computes the next semantic version, builds the Spring Boot executable JAR, and publishes it as a GitHub Release artifact.

For the first automated release, if no `v*` tags exist yet, the workflow bootstraps from the numeric part of the Gradle project version in `build.gradle`.

Example commit messages:

```text
feat: add release workflow
fix: correct watchlist normalization
feat!: remove legacy chat endpoint
```

## API

### `POST /api/chat`

Send a message and receive a response. The chat maintains conversation history, so the AI can reference previous messages in the session.

Request:

```json
{ "prompt": "Hello" }
```

Response:

```json
{ "response": "..." }
```

### `DELETE /api/chat/memory`

Clear the conversation history and start a fresh session.

Request: (no body)

Response: (204 No Content)

### `GET /api/watchlist`

List the current watchlist.

### `POST /api/watchlist`

Add a stock by ticker symbol or company name.

Request:

```json
{ "symbolOrCompanyName": "AAPL" }
```

### `DELETE /api/watchlist`

Remove a stock by ticker symbol or company name.

### `GET /api/stock-check`

Return the latest snapshot for the current watchlist, including price, change, recommendation, and explanation.

### `POST /api/stock-check/refresh`

Refresh the stock-check snapshot immediately and return the new results.

## Chat Memory

The application maintains an in-memory conversation history:

- **Storage**: In-memory (resets on app restart)
- **Scope**: Single shared session per app instance
- **Limit**: 100 messages (configurable in `InMemoryChatMemory`)
- **Behavior**: When the limit is reached, oldest messages are removed (FIFO)

The AI can reference previous messages to provide contextual responses. To start over, call `DELETE /api/chat/memory`.

See `.copilot/instructions.md` for Spring AI API patterns and implementation details.
