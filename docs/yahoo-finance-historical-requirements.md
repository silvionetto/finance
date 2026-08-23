# Yahoo Finance Historical Price Download Requirements

## 1. Goal
Enable the application to download historical stock prices from Yahoo Finance for:
- a single stock symbol provided by the user
- all stock symbols configured in the application

## 2. Functional Requirements
- The system must accept a request for historical prices by stock symbol.
- The system must support a request to download historical prices for all configured symbols.
- The system must retrieve historical OHLCV data (open, high, low, close, volume) from Yahoo Finance.
- The system must allow a configurable date range (start date and end date).
- The system must support a configurable interval (for example: 1d, 1wk, 1mo).
- The system must persist the downloaded data for later querying and analysis.
- The system must prevent duplicate records for the same symbol, date, and interval.

## 3. Validation and Error Handling
- The system must validate symbol format before requesting data.
- The system must validate date range (start date <= end date).
- If a symbol is not found on Yahoo Finance, the system must return a clear symbol-specific error.
- If Yahoo Finance is unavailable or rate-limited, the system must return a clear retryable error.
- For "all symbols" requests, the system must continue processing remaining symbols even if one fails, and report per-symbol status.

## 4. API and Operations Requirements
- Provide an endpoint or service action to download historical prices for one symbol.
- Provide an endpoint or service action to download historical prices for all configured symbols.
- Return a summary including requested symbols, successful downloads, failed downloads, and record counts.
- Support manual execution on demand.
- Support optional scheduled execution for all configured symbols.

## 5. Non-Functional Requirements
- Use the existing Spring Boot MVC architecture.
- Keep configuration externalized (no hardcoded secrets or credentials).
- Log start, completion, and failure events with symbol-level details.
- Handle external API failures gracefully without crashing the application.
- Keep processing idempotent so repeated runs do not corrupt stored data.

## 6. Acceptance Criteria
- Given a valid symbol and date range, historical prices are downloaded and stored successfully.
- Given an invalid symbol, the system returns a clear validation or not-found error.
- Given a request for all configured symbols, the system processes each symbol and returns per-symbol results.
- Given partial Yahoo Finance failures, successful symbols are still stored and failures are clearly reported.
- Re-running the same request does not create duplicate historical records.
