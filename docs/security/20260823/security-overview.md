# Security Overview

## Summary
- The workspace contains plaintext credentials in `.env`.
- Local PostgreSQL is exposed on the host with weak static credentials.
- The build pulls from a snapshot repository, which weakens supply-chain integrity.

## Key risks

### 1) Plaintext secrets in `.env`
**Evidence:** `D:\workspace\finance\.env:2-10` contains an OpenAI-compatible API key, an FMP API key, a Polygon API key, and the PostgreSQL password in cleartext.  
**Risk:** Anyone with workspace access can reuse the keys or connect to the database.

### 2) Exposed PostgreSQL with default credentials
**Evidence:** `D:\workspace\finance\docker-compose.yml:6-10` sets `POSTGRES_USER: finance`, `POSTGRES_PASSWORD: finance`, and publishes `5432:5432`. `D:\workspace\finance\src\main\resources\application.properties:7-9` defaults to the same database credentials, and `D:\workspace\finance\README.md:51-55` documents them.  
**Risk:** Any process or user that can reach the host port can authenticate to the database and read or modify persisted application data.

### 3) Mutable snapshot dependency source
**Evidence:** `D:\workspace\finance\build.gradle` and `D:\workspace\finance\settings.gradle` use `https://repo.spring.io/snapshot`, and the Spring Boot plugin is pinned to `4.1.1-SNAPSHOT`.  
**Risk:** Build inputs are not immutable, increasing the chance of unexpected or tampered dependency contents.

## Recommended next steps
- Remove secrets from repo files, rotate any exposed keys/passwords, and commit only a redacted `.env.example`.
- Replace the database defaults with a unique password and bind PostgreSQL to localhost only, or avoid publishing the port.
- Replace snapshot repositories/artifacts with released versions before sharing or deploying the project.
- Add secret scanning to catch plaintext credentials in future changes.
