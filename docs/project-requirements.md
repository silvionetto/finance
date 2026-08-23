# Finance Project Requirements

## 1. Purpose
Build a personal Spring Boot AI chat app that helps answer finance questions, resolve tickers, and fetch market data with minimal token usage.

## 2. Scope
- Web UI at `/`
- JSON chat API at `POST /api/chat`
- Clear chat memory at `DELETE /api/chat/memory`
- Ticker lookup and market-data tools for AI function calling
- Actuator endpoints for operational visibility

## 3. Functional Requirements
- The app must maintain conversation memory within the current session.
- The app must answer finance questions using the chat model and available tools.
- The app must resolve company names to tickers when possible.
- The app must fetch market data from configured providers.
- The app must surface failures clearly when data or AI calls fail.

## 4. AI Usage Rules
- Keep prompts short and structured.
- Prefer tool calls over long model context.
- Do not resend full documents or repeated instructions unless needed.
- Use concise responses and avoid unnecessary token-heavy output.
- Store reusable rules in code/config, not in prompts.

## 5. Configuration
- Externalize API keys and model settings.
- Support OpenAI-compatible configuration via environment variables.
- Keep provider URLs and catalog paths configurable.
- Do not hardcode secrets in source control.

## 6. Non-Functional Requirements
- Use the Spring MVC servlet stack.
- Keep startup simple and reliable.
- Handle external API failures without crashing the app.
- Log important request and provider events.
- Keep the UX lightweight and easy to operate.

## 7. Acceptance Criteria
- The app starts successfully with missing optional provider credentials.
- The chat UI loads and can send a prompt.
- The chat API returns a response and preserves memory.
- Ticker lookup works from the bundled catalog.
- Market-data tools return clear success or failure results.
- Configuration remains externalized.

