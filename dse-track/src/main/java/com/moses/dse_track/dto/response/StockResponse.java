package com.moses.dse_track.dto.response;

import com.moses.dse_track.model.Stock;
import com.moses.dse_track.service.FundamentalsService.StockFundamentals;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockResponse {
    private Long id;
    private String ticker;
    private String companyName;
    private String sector;
    private BigDecimal currentPrice;
    private LocalDateTime lastUpdated;

    // Fundamental ratios — null wherever the underlying inputs haven't been
    // entered yet (see Stock's fundamental-input fields). Always computed
    // fresh from the current price, never a stale cached value.
    private BigDecimal peRatio;
    private BigDecimal earningsYieldPercent;
    private BigDecimal bookValuePerShare;
    private BigDecimal pbRatio;
    private BigDecimal marketCap;
    private BigDecimal dividendYieldPercent;
    private BigDecimal payoutRatioPercent;
    private BigDecimal roePercent;
    private BigDecimal roaPercent;
    private BigDecimal debtToEquity;
    private BigDecimal currentRatio;
    private LocalDateTime fundamentalsUpdatedAt;

    // Raw inputs the ratios above are computed from — exposed so an edit form
    // can prefill with what's already saved instead of blindly overwriting it
    // (the update endpoint replaces all of these on every call).
    private BigDecimal epsTtm;
    private Long sharesOutstanding;
    private BigDecimal netIncome;
    private BigDecimal totalEquity;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal currentAssets;
    private BigDecimal currentLiabilities;
    private BigDecimal annualDividendPerShare;

    public StockResponse(Stock stock) {
        this.id = stock.getId();
        this.ticker = stock.getTicker();
        this.companyName = stock.getCompanyName();
        this.sector = stock.getSector();
        this.currentPrice = stock.getCurrentPrice();
        this.lastUpdated = stock.getLastUpdated();
    }

    public StockResponse(Stock stock, StockFundamentals f) {
        this(stock);
        this.peRatio = f.peRatio();
        this.earningsYieldPercent = f.earningsYieldPercent();
        this.bookValuePerShare = f.bookValuePerShare();
        this.pbRatio = f.pbRatio();
        this.marketCap = f.marketCap();
        this.dividendYieldPercent = f.dividendYieldPercent();
        this.payoutRatioPercent = f.payoutRatioPercent();
        this.roePercent = f.roePercent();
        this.roaPercent = f.roaPercent();
        this.debtToEquity = f.debtToEquity();
        this.currentRatio = f.currentRatio();
        this.fundamentalsUpdatedAt = stock.getFundamentalsUpdatedAt();

        this.epsTtm = stock.getEpsTtm();
        this.sharesOutstanding = stock.getSharesOutstanding();
        this.netIncome = stock.getNetIncome();
        this.totalEquity = stock.getTotalEquity();
        this.totalAssets = stock.getTotalAssets();
        this.totalLiabilities = stock.getTotalLiabilities();
        this.currentAssets = stock.getCurrentAssets();
        this.currentLiabilities = stock.getCurrentLiabilities();
        this.annualDividendPerShare = stock.getAnnualDividendPerShare();
    }
}
