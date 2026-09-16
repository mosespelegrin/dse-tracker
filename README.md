# DSE Track

A portfolio tracker for the Dar es Salaam Stock Exchange — buy/sell transactions, holdings, price alerts, dividends, fundamental analysis (P/E, P/B, ROE, dividend yield, etc.), CSV order-import reconciliation, and CSV/PDF export.

- **Backend**: Spring Boot 4.1 (Java 21), MySQL, JWT auth, Flyway migrations
- **Frontend**: Angular 22, standalone components

## Project layout

```
dse-track/    Spring Boot backend
frontend/     Angular frontend
docker-compose.yml   Full stack (mysql + backend + frontend)
```

## Running locally (without Docker)

**Prerequisites**: Java 21, Node 22+, a running MySQL instance with a `dse_tracker` database created.

```bash
# Backend (http://localhost:8081)
cd dse-track
JWT_SECRET="a-real-random-32-byte-string" ./mvnw spring-boot:run

# Frontend (http://localhost:4200), in a second terminal
cd frontend
npm install
npm start
```

See **Environment variables** below for everything else you can/should set (mail, DB credentials, etc.) — everything has a working localhost default except `JWT_SECRET`.

## Running the tests

```bash
cd dse-track
./mvnw test
```

Tests run against an in-memory H2 database — they never touch your real MySQL instance. Includes unit tests for the trickiest business logic (fundamentals math, portfolio P&L, buy/sell/cost-basis, login rate limiting, JWT) plus one integration test that boots the full app and exercises register → login → protected endpoint end-to-end.

## Running with Docker (recommended for production)

```bash
cp .env.example .env
# fill in .env — JWT_SECRET, DB_PASSWORD, MAIL_USERNAME/PASSWORD, FRONTEND_URL, API_URL, CORS_ALLOWED_ORIGINS
docker compose up -d --build
```

This starts MySQL, the backend, and the frontend (served via nginx) — each with an explicit memory limit (`mem_limit` in `docker-compose.yml`) and a JVM configured to respect the container's memory limit rather than the host's (see comments in `dse-track/Dockerfile`). Adjust the `mem_limit` values if your server has more/less RAM to spare.

The frontend image is built once and reads its backend URL at **container startup**, not build time — `API_URL` in `.env` controls where it points, so you don't need to rebuild the frontend image to point it at a different backend.

## Google Sign-In (optional)

1. Go to [console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials) → **Create Credentials** → **OAuth client ID** → **Web application**.
2. Under **Authorized JavaScript origins**, add `http://localhost:4200` (dev) and your real frontend domain (prod). No redirect URI is needed — this uses Google Identity Services' ID-token flow, not a redirect-based flow.
3. Copy the **Client ID** (not the secret — it isn't needed) into `GOOGLE_CLIENT_ID`, set on **both** the backend and the frontend container (see `.env.example`).

Without it set, the "Sign in with Google" button just shows a small "not configured" note instead of erroring.

## Email verification

Registering always sends a verification email; whether it's actually *required* to log in is controlled by `REQUIRE_EMAIL_VERIFICATION` (off by default, so existing accounts aren't locked out). A blocked login shows a "Resend verification email" option in the UI.

## Database schema

Managed by [Flyway](https://flywaydb.org/) (`dse-track/src/main/resources/db/migration/`) — `spring.jpa.hibernate.ddl-auto` is set to `validate`, meaning Hibernate checks the schema matches but never changes it itself.

**If you're upgrading an existing dev database** that was built by the old `ddl-auto=update` behavior: start the app once *before* pulling this change (or check out the last commit that still had `ddl-auto=update`) to let Hibernate apply any pending column additions, confirm it starts cleanly, then pull this change. Flyway's `baseline-on-migrate` will adopt your existing schema as version 1 without trying to re-run `V1__init_schema.sql` against it. A fresh/empty database (e.g. in CI or a new prod deploy) runs `V1__init_schema.sql` in full automatically.

## Environment variables

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `JWT_SECRET` | **Yes, in any shared environment** | dev placeholder | Signs JWTs — must be a real random 32+ byte string outside localhost |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/dse_tracker` | JDBC URL |
| `DB_USERNAME` | No | `root` | MySQL username |
| `DB_PASSWORD` | No | *(empty)* | MySQL password |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | No (emails just silently fail without it) | *(empty)* | SMTP credentials for alert/verification/reset emails |
| `FRONTEND_URL` | No | `http://localhost:8081` | Used to build links inside emails |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:8081,http://localhost:4200` | Comma-separated list of origins allowed to call the API |
| `API_URL` (frontend container only) | No | `http://localhost:8081` | Backend URL the frontend calls |
| `GOOGLE_CLIENT_ID` (both backend and frontend) | No | *(empty — button hidden)* | OAuth Client ID for "Sign in with Google" — see below |
| `REQUIRE_EMAIL_VERIFICATION` | No | `false` | Blocks login until the emailed verification link is clicked |
| `DB_POOL_MAX`, `TOMCAT_MAX_THREADS` | No | `10`, `50` | Resource-limiting knobs — raise only if you actually need more |

## Known limitations

- No role/admin system — any authenticated user can edit any stock's fundamentals or other shared data. Fine for personal use, not for multi-tenant hosting.
- Fundamental-analysis inputs (EPS, book value, etc.) and stock dividends are manual entry — there's no public feed for DSE-listed company financials to scrape.
- The DSE price-scraper endpoint (`DsePriceScraperService`) was inherited, pre-existing code — its target URL hasn't been independently re-verified as still live.
