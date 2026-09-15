package com.moses.dse_track.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "current_price", precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(length = 100)
    private String sector;

    // ── Fundamental inputs ──────────────────────────────────────────────
    // Raw company financials, entered manually (there's no public per-company
    // fundamentals feed for DSE-listed stocks — only the price scraper is
    // automated). FundamentalsService derives every ratio (P/E, P/B, ROE, ...)
    // from these plus currentPrice, computed fresh on every read — never cached,
    // so they're automatically up to date the instant the price scraper runs.

    @Column(name = "eps_ttm", precision = 15, scale = 4)
    private BigDecimal epsTtm; // trailing-twelve-month earnings per share

    @Column(name = "shares_outstanding")
    private Long sharesOutstanding;

    @Column(name = "net_income", precision = 18, scale = 2)
    private BigDecimal netIncome; // trailing-twelve-month

    @Column(name = "total_equity", precision = 18, scale = 2)
    private BigDecimal totalEquity;

    @Column(name = "total_assets", precision = 18, scale = 2)
    private BigDecimal totalAssets;

    @Column(name = "total_liabilities", precision = 18, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "current_assets", precision = 18, scale = 2)
    private BigDecimal currentAssets;

    @Column(name = "current_liabilities", precision = 18, scale = 2)
    private BigDecimal currentLiabilities;

    @Column(name = "annual_dividend_per_share", precision = 15, scale = 4)
    private BigDecimal annualDividendPerShare; // company's indicated annual rate — distinct from the per-user Dividend log

    @Column(name = "fundamentals_updated_at")
    private LocalDateTime fundamentalsUpdatedAt;
}