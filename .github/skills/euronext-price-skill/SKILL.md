---
name: euronext-price-skill
description: Read a Euronext product page or product identifier and return the latest quoted price, quote timestamp, and a short market summary using Euronext public market data.
---

# Euronext Price Reader

## Use when

- The user provides a Euronext product URL such as `https://live.euronext.com/en/product/equities/NL0011540547-XAMS`.
- The user asks for the latest price from a Euronext Live product page.
- The user wants a concise quote summary for a Euronext-listed instrument.

## Supported input

- A full Euronext Live product URL.
- A `product_data` identifier like `NL0011540547-XAMS`.
- An ISIN plus MIC when the user already knows them.

## Workflow

1. Accept the Euronext input and normalize it to:
   - `product_data` in the form `<ISIN>-<MIC>`
   - `isin`
   - `mic`
2. If the user gave a full URL, fetch the product page first and confirm the instrument metadata from the page when possible.
3. Prefer Euronext's public instrument detail API for the quote:
   - `https://gateway.euronext.com/api/instrumentDetail?code=<ISIN>&codification=ISIN&exchCode=<MIC>&sessionQuality=RT&view=FULL&authKey=256f0720127269acfcb390b5adef10c242469c41afd08426744324bee3e2d75a`
4. Parse the JSON response from `instr.currInstrSess` and use these fields when present:
   - latest price: `lastPx`
   - quote timestamp: `lastUpdate`
   - currency: `instr.currency`
   - open: `openPx`
   - previous close: `prevAdjClosingPrice`
   - traded quantity: `tradedQty`
   - trade count: `nbTrades`
   - VWAP: `vwap`
   - trading status: `instrTradingStatus`
   - market capitalization: `marketCapitalisation`
5. Build a short market summary from the available fields, preferring:
   - open vs last price
   - previous close vs last price
   - traded quantity and number of trades
   - VWAP
   - trading status when relevant

## Fallback

If the gateway API is unavailable or does not return usable quote data:

1. Fetch the Euronext product page.
2. Extract `product_data` and the page decryption key from `drupalSettings.ajax_secure.kye`.
3. Use the page's AJAX endpoints as best-effort fallbacks, especially:
   - `/en/ajax/getDetailedQuote/<product_data>`
   - `/en/ajax/getIntradayPrice/<product_data>`
   - `/en/intraday_chart/getChartData/<product_data>/intraday`
4. If the response is encrypted JSON with `ct`, `iv`, and `s`, decrypt it using the page key before parsing.
5. Use the newest timestamped price data you can verify.

## Output

- Return the instrument name and exchange.
- Return the latest quoted price with currency.
- Return the quote timestamp.
- Return a short market summary in 1 or 2 sentences.
- Add a brief best-effort note if you had to use fallback extraction or if any fields were unavailable.

## Examples

- `Get the latest Euronext quote for https://live.euronext.com/en/product/equities/NL0011540547-XAMS`
- `What is the latest price for NL0011540547-XAMS?`
- `Get a Euronext quote for ABN.AS`

Example response shape:

- `ABN AMRO BANK N.V. (Euronext Amsterdam): EUR 43.08, quoted at 2026-09-14T17:55:01. Open 43.28, previous close 43.52, VWAP 43.0616, 1,448,724 shares across 4,229 trades.`

## Rules

- Never invent quote values.
- Prefer structured JSON over scraping rendered text.
- Treat third-party page structure and endpoints as best-effort and say so when confidence is reduced.
- Keep the response concise and factual.
