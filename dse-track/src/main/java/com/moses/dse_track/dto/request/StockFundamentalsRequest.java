package com.moses.dse_track.dto.request;

import lombok.Data;

import java.math.BigDecimal;

// Raw company financials — all optional, since a stock's fundamentals are
// usually filled in gradually as annual/quarterly reports become available.
@Data
public class StockFundamentalsRequest {

    private BigDecimal epsTtm;
    private Long sharesOutstanding;
    private BigDecimal netIncome;
    private BigDecimal totalEquity;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal currentAssets;
    private BigDecimal currentLiabilities;
    private BigDecimal annualDividendPerShare;
}
