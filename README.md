# Finance

Spring Boot 4.1 application for a personal AI chat UI backed by Spring AI OpenAI.

## What it does

- Serves a chat page at `/`
- Exposes a JSON chat API at `POST /api/chat`
- Uses Thymeleaf for the web UI
- Provides actuator endpoints via Spring Boot Actuator

## Requirements

- Java 25
- Gradle
- An OpenAI-compatible API key in `OPENAI_API_KEY` or `AZURE_OPENAI_API_KEY`

## Configuration

`src/main/resources/application.properties` supports:

- `spring.application.name=finance`
- `spring.ai.openai.api-key`
- `spring.ai.openai.base-url`
- `spring.ai.openai.chat.options.model`

Environment variables can override the AI settings:

- `OPENAI_API_KEY`
- `AZURE_OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`

The current defaults are:

- API key: `OPENAI_API_KEY`, falling back to `AZURE_OPENAI_API_KEY`
- Base URL: `https://api.openai.com/v1`
- Chat model: `gpt-4o-mini`

For a local or alternate OpenAI-compatible provider, set `OPENAI_BASE_URL` and `OPENAI_MODEL` to match that service.

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

Request:

```json
{ "prompt": "Hello" }
```

Response:

```json
{ "response": "..." }
```
