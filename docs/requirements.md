# Spring Boot AI Application Requirements

## 1. Application Goal
Build a personal Spring Boot web application that periodically checks stock-market web pages, retrieves stock prices, analyzes changes against a watchlist, and notifies the user with buy, sell, or hold guidance.

The application is for one user only and should focus on practical decision support, not automated trading.

## 2. AI Capabilities
- Summarize stock movements and watchlist status in plain language.
- Answer questions about watched stocks and recent price changes.
- Extract structured price and market data from external web pages or API responses.
- Generate concise buy, sell, or hold recommendations based on configured rules and retrieved data.
- Orchestrate external calls needed to fetch, normalize, and analyze stock information.

## 3. API Contract
- Provide a web UI as the main user interface.
- Expose endpoints for viewing watchlist items, latest stock checks, and AI-generated recommendations.
- Support a manual refresh action to trigger a new market check.
- Return clear validation and error responses when data cannot be retrieved or analyzed.

## 4. Configuration
- Store Spring AI and external market-source settings in application configuration.
- Support configuration for at least:
  - external source URLs or API endpoints
  - watchlist entries
  - refresh frequency
  - recommendation thresholds
- Keep secrets out of source code and load them from environment variables or externalized config.

## 5. Non-Functional Requirements
- Use the existing Spring Boot servlet stack and Spring MVC.
- Keep the application simple, personal, and easy to operate.
- Log important fetch and analysis events.
- Handle external-source failures without crashing the application.
- Avoid silent failures; show actionable errors in the UI or API.
- Keep recommendations explainable so the user can understand why a signal was produced.

## 6. Acceptance Criteria
- The app can show a watchlist in the UI.
- The app can fetch stock data from an external source.
- The app can analyze retrieved prices and produce a recommendation.
- The app can explain the reason behind a recommendation.
- Configuration is externalized and does not hardcode secrets.
- Failures in external data retrieval are reported clearly.

