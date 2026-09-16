# Feature Roadmap

Planned features for DSE Track, grouped by priority. Numbering follows the
original backlog this was pulled from, so items 1, 2, and 4 aren't listed
here (already shipped or tracked elsewhere).

---

## 🟡 Medium-High Priority

### 3. Annual Returns Summary

A year-by-year performance breakdown, combining capital gains, dividends,
and fees into one net-return figure.

```
2024 Performance:
  Capital gains:    +TZS 450,000  (+12.3%)
  Dividends:        +TZS 180,000  (+4.9%)
  Broker fees paid: -TZS  45,000  (-1.2%)
  Net return:       +TZS 585,000  (+15.9%)

Best stock:   TBL    +28%
Worst stock:  NICOL   -4%
```

**Why it matters:** Gives investors a clear year-by-year view for personal
financial planning.

**Build notes:**
- Aggregate transactions + dividends by year.
- New endpoint: `GET /portfolio/annual-summary?year=2026`.

---

### 5. DSE News Feed

Scrape DSE announcements and company news alongside the existing price
scraper.

```
📢 CRDB Bank announces TZS 45 dividend per share
📢 NMB Bank Q1 profits up 18%
📢 TBL rights issue — 1 new share for every 5 held
```

**Why it matters:** Investors currently rely on WhatsApp groups and word of
mouth for DSE news.

**Build notes:**
- New `@Scheduled` job, similar to `DsePriceScraperService`.
- New `news` table + `NewsController`.

---

## 🟠 Medium Priority

### 6. Rights Issue Tracker

DSE companies frequently do rights issues. This confuses many investors.

```
TBL Rights Issue:
  You own: 500 TBL shares
  Offer:   1 new share for every 5 held
  You can buy: 100 new TBL shares
  At price: TZS 1,800 per share
  Total cost: TZS 180,000
  Deadline: 30 July 2026

  [Accept Rights Issue] [Decline]
```

**Why it matters:** Investors often miss rights issues because they don't
understand them or forget the deadline.

**Build notes:**
- New `RightsIssue` entity (global, one row per company announcement — same
  shape as `Stock`'s fundamentals fields): `stock`, `newSharesPerHeld` /
  `heldSharesRequired` (the "1 for every 5" ratio), `pricePerShare`,
  `deadline`, `createdAt`. New `rights_issues` table + `RightsIssueRepository`.
- New `RightsIssueResponse` entity to record each user's decision:
  `user`, `rightsIssue`, `status` (`PENDING`/`ACCEPTED`/`DECLINED`),
  `sharesAccepted`, `decidedAt`.
- New `RightsIssueService`:
  - Entitlement is computed from the user's current `Holding`, not stored:
    `entitledShares = holding.shares / heldSharesRequired * newSharesPerHeld`
    (integer division, matching how DSE rounds down fractional entitlements).
  - `acceptRightsIssue(userId, rightsIssueId)` — validates the deadline
    hasn't passed, then calls the existing `TransactionService.buy(...)`
    with the entitled shares and `pricePerShare` so the new position lands
    in the user's holdings the same way any other buy would, and records
    the `RightsIssueResponse`.
  - `declineRightsIssue(userId, rightsIssueId)` — just records the response.
- New `RightsIssueController`:
  - `GET /rights-issues` — active issues, each annotated with the caller's
    computed entitlement (0 if they don't hold the stock).
  - `POST /rights-issues/{id}/accept`
  - `POST /rights-issues/{id}/decline`
  - `POST /rights-issues` — manual entry of a new announcement (no DSE feed
    for this exists yet, same as stock fundamentals today). This is global,
    shared data like `PUT /stocks/{id}/fundamentals` — decide on an
    authorization story for who's allowed to create/edit these before
    shipping, rather than leaving it open to any authenticated user.
- Deadline reminders: reuse the `@Scheduled` + `EmailService` pattern
  already used for triggered alerts — a daily job emails anyone with an
  un-actioned `RightsIssueResponse` (or no response at all) as the
  deadline approaches.

---

### 7. Investment Goals

> Largely covered by the existing Alerts feature for now — e.g. "notify me
> when portfolio value exceeds TZS 5,000,000." A dedicated Goals feature
> would add progress tracking on top of that.

```
Moses sets a goal: "I want to earn TZS 2,000,000 in dividends per year"

System shows:
  Current dividend income: TZS 800,000/year
  Gap:                     TZS 1,200,000 remaining
  Suggested:                Buy 2,000 more NMB shares
```

**Build notes:**
- New `Goal` entity: `user`, `goalType` (start with `ANNUAL_DIVIDEND_INCOME`;
  leave room for `PORTFOLIO_VALUE`, `TOTAL_RETURN` later), `targetAmount`,
  `createdAt`. New `goals` table + `GoalRepository`.
- New `GoalService`:
  - `getProgress(userId, goalId)` — for `ANNUAL_DIVIDEND_INCOME`, reuses
    `DividendService`'s history query filtered to the trailing 12 months
    (a new `findByUserIdAndPaymentDateAfter` repository method) to get
    current income, then `gap = target - current`.
  - "Suggested" line is a simple heuristic, not a recommendation engine:
    pick the held stock with the highest `annualDividendPerShare` (from
    `FundamentalsService`/`Stock`) and compute
    `sharesNeeded = ceil(gap / stock.annualDividendPerShare)`.
- New `GoalController`: `POST /goals`, `GET /goals`, `GET /goals/{id}`
  (includes computed progress), `DELETE /goals/{id}`.
- Overlap with Alerts: once a goal is met, optionally create a one-off
  triggered notification through the existing `EmailService`
  (`sendAlertTriggeredEmail`-style template) rather than building a second
  notification path — defer unless a user actually asks for it.

---

### 8. M-Pesa / Mobile Money Tracker

Tanzanian investors fund their broker accounts via M-Pesa, Tigo Pesa, Airtel
Money.

```
User logs: "Sent TZS 500,000 to Orbit Securities via M-Pesa"

System tracks:
  Cash deposited:  TZS 500,000
  Cash invested:   TZS 480,000
  Cash remaining:  TZS 20,000   ← available to invest
```

**Why it matters:** Investors lose track of how much cash they have sitting
idle at their broker.

**Build notes:**
- New `CashDeposit` entity: `user`, `provider` (`M_PESA` / `TIGO_PESA` /
  `AIRTEL_MONEY` / `BANK`), `amount`, `broker` (free text for now — there's
  no broker field on `Transaction`/`Holding` yet to link against), `date`,
  `notes`. New `cash_deposits` table + `CashDepositRepository`, following
  the same manual-entry pattern as `DividendService`/`recordDividend`.
- New `CashTrackerService.getCashSummary(userId)`:
  - `deposited` = sum of `CashDeposit.amount`.
  - `invested` = sum of `BUY.totalPaid` minus sum of `SELL.totalPaid` across
    the user's transactions (net cash currently deployed in the market) —
    reuse `TransactionRepository.findByUserIdOrderByDateDesc` the same way
    `PortfolioService.getTotalRealizedGain` already aggregates transactions.
  - `remaining` = `deposited - invested`.
- New `CashController`: `POST /cash/deposits`, `GET /cash/deposits`,
  `GET /cash/summary`.
- Out of scope for v1: reconciling cash against a specific broker's actual
  balance, or handling withdrawals back out of the broker — both would need
  a `type` (`DEPOSIT`/`WITHDRAWAL`) on `CashDeposit` (better named
  `CashMovement` at that point) rather than assuming money only flows in.
