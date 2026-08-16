# Finance

Spring Boot 4.1 application for a personal AI chat UI backed by Spring AI OpenAI.

## What it does

- Serves a chat page at `/`
- Exposes a JSON chat API at `POST /api/chat` with **conversation memory** support
- Maintains chat history within a session for contextual responses
- Uses Thymeleaf for the web UI
- Provides actuator endpoints via Spring Boot Actuator
- Integrates AI tools (e.g., date/time functions, ticker lookup, market prices) for function calling

## Requirements

- Java 25
- Gradle
- An OpenAI-compatible API key in `OPENAI_API_KEY` or `AZURE_OPENAI_API_KEY`
- A Polygon.io API key in `POLYGON_API_KEY` for market price lookups

## Configuration

`src/main/resources/application.properties` supports:

- `spring.application.name=finance`
- `spring.ai.openai.api-key`
- `spring.ai.openai.base-url`
- `spring.ai.openai.chat.options.model`
- `polygon.api-key`
- `polygon.base-url`

Environment variables can override the AI settings:

- `OPENAI_API_KEY`
- `AZURE_OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `POLYGON_API_KEY`
- `POLYGON_BASE_URL`

The current defaults are:

- API key: `OPENAI_API_KEY`, falling back to `AZURE_OPENAI_API_KEY`
- Base URL: `https://api.openai.com/v1`
- Chat model: `gpt-4o-mini`

For a local or alternate OpenAI-compatible provider, set `OPENAI_BASE_URL` and `OPENAI_MODEL` to match that service.

The Polygon tool currently uses the `v1/open-close/{symbol}/{date}` endpoint to return open and close prices for a trading day. Provide dates in `YYYY-MM-DD` format.

## Run

```bash
gradlew.bat bootRun
```

Then open `http://localhost:8080`.

## Build

```bash
gradlew.bat build
```

## Test

```bash
gradlew.bat test
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

## Chat Memory

The application maintains an in-memory conversation history:

- **Storage**: In-memory (resets on app restart)
- **Scope**: Single shared session per app instance
- **Limit**: 100 messages (configurable in `InMemoryChatMemory`)
- **Behavior**: When the limit is reached, oldest messages are removed (FIFO)

The AI can reference previous messages to provide contextual responses. To start over, call `DELETE /api/chat/memory`.

See `.copilot/instructions.md` for Spring AI API patterns and implementation details.
