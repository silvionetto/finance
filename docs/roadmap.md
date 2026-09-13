# Finance roadmap

This document tracks the product direction for the finance app and should stay aligned with the codebase as features are added or completed.

## Current status

### Implemented

- Chat UI at `/` using Thymeleaf
- JSON chat API at `POST /api/chat`
- In-memory conversation memory with `DELETE /api/chat/memory`
- Watchlist API at `/api/watchlist`
- Stock ticker lookup using the local catalog, Polygon, and Financial Modeling Prep fallback
- Polygon market data lookup tool
- Latest stock-check snapshot API at `/api/stock-check`
- Manual stock-check refresh endpoint at `/api/stock-check/refresh`
- UI rendering for the latest stock-check snapshot and recommendation explanation
- Rules-based buy / hold / sell recommendation logic for current watchlist items
- PostgreSQL persistence for watchlist entries and the company ticker catalog
- Flyway-based database migrations
- Spring AI tool integration for chat actions

### Planned next

- Periodic stock-market checks for watchlist items
- Plain-language summaries of watchlist status and price movement
- Persisting stock-check history instead of keeping only the latest in memory
- Notification delivery for changes in watchlist signals

### Review follow-ups

- Gate BRAPI quote routing in `TickerLookupTool` behind provider configuration so Brazilian-looking symbols still fall back to Financial Modeling Prep when BRAPI is disabled
- Register BRAPI and Alpaca chat tools only when those providers are configured so the model does not select guaranteed-failure tools
- Add eviction or expiry for the session-keyed in-memory maps in `InMemoryChatMemory` and `StockCheckService` to avoid unbounded growth on long-running servers
- Reduce `StockCheckService` lock scope so external quote fetches do not block all sessions behind one slow provider call
- Introduce a market-data provider router abstraction to centralize provider configuration, symbol support, ticker resolution, quote fallback, and chat-tool exposure

### Later ideas

- Persistent chat history instead of in-memory storage
- Conversation IDs and multi-session support
- Search and filtering for watchlist and market history
- More advanced scoring, thresholds, and alert rules
- Better handling of long-running refresh jobs and external data failures

## Sync notes

- Keep this file updated when endpoints, tools, or persistence change.
- Move items between sections when they are implemented or removed from scope.
- Prefer short, code-anchored bullets over speculative product prose.
