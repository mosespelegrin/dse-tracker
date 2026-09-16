# DSE Track — About & Testing Guide

## What this is

**DSE Track** is a portfolio tracker for the **Dar es Salaam Stock Exchange (DSE)** — built for retail investors who want one place to see what they own, what it's worth, and how it's doing, instead of maintaining spreadsheets by hand.

### Core features

- **Accounts** — email/password registration and login, Google Sign-In, email verification, password reset, JWT access tokens + refresh tokens
- **Transactions** — log buy/sell trades; the app tracks weighted-average cost basis automatically
- **Portfolio** — current holdings, unrealized gain/loss (once you enter today's prices), realized gain/loss (from past sells), sector allocation breakdown
- **Price alerts** — set a target price (above/below) per stock; get emailed when it triggers
- **Dividends** — log dividend payments received per holding, see lifetime dividend income
- **Fundamental analysis** — P/E, P/B, ROE, ROA, dividend yield, debt-to-equity, current ratio, market cap — computed live from the current price plus financials you enter (there's no public feed for these, so it's manual entry, same as dividends)
- **CSV order-import reconciliation** — upload a broker/DSE order export and see which rows match what you've logged, which don't, and which of your own transactions weren't in the file
- **CSV/PDF export** — download your transaction and dividend history
- **Shareable portfolio card** — a branded image of your % return, downloadable/postable, with no dollar amounts shown (privacy by design)

### Tech stack

- **Backend**: Spring Boot 4.1 (Java 21), MySQL, Spring Security + JWT, Flyway migrations
- **Frontend**: Angular 22, standalone components, no external UI framework

---

## Prerequisites

- Backend running on `http://localhost:8081` (see the root `README.md` for setup)
- Frontend running on `http://localhost:4200` (`cd frontend && npm start`) — only needed for the frontend walkthrough, not for Postman
- [Postman](https://www.postman.com/downloads/) — only needed for the API walkthrough

---

## Part 1 — Testing in the frontend

Open `http://localhost:4200`.

### 1. Landing page
You should see a marketing homepage (hero, feature cards, sample market table, testimonial) — this is the public entry point, not gated behind login.

### 2. Create an account
Click **Sign up**. Either:
- Fill in name/email/password and submit, or
- Click the Google button (only works if `GOOGLE_CLIENT_ID` has been configured — see root `README.md`)

You'll land on the dashboard. Check your email (if `MAIL_USERNAME`/`MAIL_PASSWORD` are set) for a verification link — logging in doesn't require clicking it unless `REQUIRE_EMAIL_VERIFICATION=true`.

### 3. Portfolio (empty at first)
`Dashboard → Portfolio` — shows nothing until you log a transaction. Also has a **sector breakdown** section that fills in once you hold something.

### 4. Log a transaction
`Dashboard → Transactions` → fill in the **Buy** form (pick a stock, shares, total paid, date) → submit. Switch to **Sell** to test a partial sell later. Your **Portfolio** page should now show the holding.

### 5. Calculator — P&L, chart, and sharing
`Dashboard → Calculator` → enter a "current price" for a holding → **Calculate**. You should see:
- Total current value / gain-loss / ROI stat cards
- A **gain/loss bar chart** per holding (bars extend right for gains, left for losses)
- A **Share your return** button — opens a downloadable branded image card

### 6. Alerts
`Dashboard → Alerts` → create one (pick a stock, condition, target price) → it appears in the list as "Watching". It flips to "Triggered" automatically the next time the scheduled DSE price fetch runs and crosses your target (or you can trigger a fetch manually — see Postman section).

### 7. Dividends
`Dashboard → Dividends` → log one (stock, amount/share, shares held, date) → see it in history and reflected in "Lifetime dividend income".

### 8. Fundamentals
`Dashboard → Fundamentals` → pick a stock → **Edit inputs** → enter any financials you know (EPS, equity, shares outstanding, etc.) → save. The table's P/E, P/B, ROE, etc. update immediately using the stock's current price.

### 9. CSV import
Back on **Transactions**, use the **Import DSE order export** file input to upload a CSV. You'll get a per-row reconciliation report (matched / mismatched / not found).

### 10. Export
Also on Transactions/Dividends — **Export CSV** / **Export PDF** buttons download your history.

### 11. Password reset
Log out → **Forgot password?** on the login page → enter your email → check inbox for the token → **Reset password** page (the link in the email opens this pre-filled).

---

## Part 2 — Testing with Postman

No collection file needed — every request below is copy-pasteable. Base URL: `http://localhost:8081`.

### Setting up auth once

1. Run **Register** or **Login** below.
2. In that request's **Tests** tab, add:
   ```js
   const data = pm.response.json();
   pm.environment.set("token", data.token);
   pm.environment.set("refreshToken", data.refreshToken);
   ```
3. Create a Postman **Environment** (top-right dropdown → New Environment) with blank `token` and `refreshToken` variables.
4. On every protected request below, set header `Authorization: Bearer {{token}}` — it fills in automatically once you've run Register/Login/Refresh/Google once.

### Auth

**Register** (public)
```
POST /auth/register
{ "name": "Moses Pelegrin", "email": "moses@example.com", "password": "password123" }
```

**Login** (public)
```
POST /auth/login
{ "email": "moses@example.com", "password": "password123" }
```

**Refresh token** (public)
```
POST /auth/refresh
{ "refreshToken": "{{refreshToken}}" }
```

**Logout** (public — revokes the given refresh token)
```
POST /auth/logout
{ "refreshToken": "{{refreshToken}}" }
```

**Forgot password** (public — always returns the same message, whether or not the email exists)
```
POST /auth/forgot-password
{ "email": "moses@example.com" }
```

**Reset password** (public)
```
POST /auth/reset-password
{ "token": "PASTE_FROM_EMAIL", "newPassword": "newpassword123" }
```

**Resend verification email** (public)
```
POST /auth/resend-verification
{ "email": "moses@example.com" }
```

**Verify email** (public — normally clicked from the email link, opens an HTML confirmation)
```
GET /auth/verify-email?token=PASTE_FROM_EMAIL
```

**Google Sign-In** (public — needs a real ID token from Google Identity Services, so this one's hard to fire from Postman directly; test it via the frontend button instead)
```
POST /auth/google
{ "idToken": "..." }
```

### Stocks

**List all stocks** (public)
```
GET /stocks
```

**Get one stock (with fundamentals)**
```
GET /stocks/1
Authorization: Bearer {{token}}
```

**Set a stock's fundamentals inputs**
```
PUT /stocks/1/fundamentals
Authorization: Bearer {{token}}
{
  "epsTtm": 500,
  "sharesOutstanding": 1000000,
  "netIncome": 500000000,
  "totalEquity": 5000000000,
  "totalAssets": 20000000000,
  "totalLiabilities": 15000000000,
  "currentAssets": 3000000000,
  "currentLiabilities": 1500000000,
  "annualDividendPerShare": 100
}
```

### Transactions

**Buy**
```
POST /transactions/buy
Authorization: Bearer {{token}}
{ "stockId": 1, "shares": 100, "totalPaid": 500000, "date": "2026-09-01", "notes": "first buy" }
```

**Sell**
```
POST /transactions/sell
Authorization: Bearer {{token}}
{ "stockId": 1, "shares": 50, "sellPrice": 5500, "date": "2026-09-10", "notes": "partial sell" }
```

**History**
```
GET /transactions
Authorization: Bearer {{token}}
```

**History, paginated**
```
GET /transactions/page?page=0&size=20
Authorization: Bearer {{token}}
```

**Import a CSV order export** (multipart, not JSON)
```
POST /transactions/import
Authorization: Bearer {{token}}
Body: form-data, key "file" (type: File), pick a .csv
```

**Export**
```
GET /transactions/export.csv
GET /transactions/export.pdf
Authorization: Bearer {{token}}
```
(Postman shows these as binary — "Save Response → Save to a file" to download.)

### Portfolio

**Holdings**
```
GET /portfolio
Authorization: Bearer {{token}}
```

**Calculate P&L** (body maps stockId → current price)
```
POST /portfolio/calculate
Authorization: Bearer {{token}}
{ "1": 5800, "2": 300 }
```

**Sector breakdown**
```
GET /portfolio/sectors
Authorization: Bearer {{token}}
```

### Alerts

**Create**
```
POST /alerts
Authorization: Bearer {{token}}
{ "stockId": 1, "conditionType": "ABOVE", "targetPrice": 6000 }
```

**List**
```
GET /alerts
Authorization: Bearer {{token}}
```

**Delete**
```
DELETE /alerts/1
Authorization: Bearer {{token}}
```

### Dividends

**Log one**
```
POST /dividends
Authorization: Bearer {{token}}
{ "stockId": 1, "amountPerShare": 50, "shares": 50, "paymentDate": "2026-08-15", "notes": "Q2 dividend" }
```

**History / total / paginated / export**
```
GET /dividends
GET /dividends/total
GET /dividends/page?page=0&size=20
GET /dividends/export.csv
GET /dividends/export.pdf
Authorization: Bearer {{token}}
```

### Health check (public — no auth needed)
```
GET /actuator/health
```
Should return `{"status":"UP"}` once the backend and database are both reachable.

---

## Known gaps worth knowing about while testing

- No role/admin system — any logged-in user can edit any stock's shared fundamentals data.
- Fundamentals and dividends are manual entry — nothing scrapes real DSE company financials.
- The scheduled DSE price fetch runs weekdays at 1pm (Africa/Dar_es_Salaam) — there's no manual "trigger now" endpoint exposed, so alerts only re-check on that schedule.
