---
name: company-ticker-skill
description: Resolve a company name to a ticker symbol using live public market data. Use when the user asks for a ticker from a company name or needs a best-effort market listing match.
---

# Company to Ticker

## Workflow

1. Accept a company name.
2. Search live public market data for likely matching listings.
3. Prefer the closest company name match across global exchanges.
4. Return the ticker and a short note about confidence or ambiguity.

## Output

- Return the ticker symbol.
- Add a short note when the match is best-effort or ambiguous.
- If no clear match exists, say so briefly.

## Rules

- Use live public lookup data.
- Support global best-effort resolution.
- Keep the response concise and factual.
