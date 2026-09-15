package com.moses.dse_track.service;

import com.moses.dse_track.model.Stock;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Computes standard fundamental-analysis ratios from a Stock's currentPrice
// plus its manually-entered financial inputs. Every ratio is computed fresh
// on every call — nothing is cached — so a ratio is always consistent with
// whatever price/inputs are currently stored; no invalidation to get wrong.
// Any ratio whose required inputs are missing or whose denominator is zero
// comes back null rather than a fabricated or misleading number.
@Service
public class FundamentalsService {

    private static final int RATIO_SCALE = 4;
    private static final int PERCENT_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public record StockFundamentals(
            BigDecimal peRatio,
            BigDecimal earningsYieldPercent,
            BigDecimal bookValuePerShare,
            BigDecimal pbRatio,
            BigDecimal marketCap,
            BigDecimal dividendYieldPercent,
            BigDecimal payoutRatioPercent,
            BigDecimal roePercent,
            BigDecimal roaPercent,
            BigDecimal debtToEquity,
            BigDecimal currentRatio
    ) {}

    public StockFundamentals compute(Stock stock) {
        BigDecimal price = stock.getCurrentPrice();
        BigDecimal eps = stock.getEpsTtm();
        BigDecimal shares = stock.getSharesOutstanding() != null
                ? BigDecimal.valueOf(stock.getSharesOutstanding()) : null;
        BigDecimal netIncome = stock.getNetIncome();
        BigDecimal equity = stock.getTotalEquity();
        BigDecimal assets = stock.getTotalAssets();
        BigDecimal liabilities = stock.getTotalLiabilities();
        BigDecimal currentAssets = stock.getCurrentAssets();
        BigDecimal currentLiabilities = stock.getCurrentLiabilities();
        BigDecimal annualDividend = stock.getAnnualDividendPerShare();

        BigDecimal bookValuePerShare = divide(equity, shares, RATIO_SCALE);

        return new StockFundamentals(
                divide(price, eps, RATIO_SCALE),                                  // P/E
                percent(eps, price),                                              // earnings yield
                bookValuePerShare,
                divide(price, bookValuePerShare, RATIO_SCALE),                    // P/B
                multiply(price, shares),                                          // market cap
                percent(annualDividend, price),                                   // dividend yield
                percent(annualDividend, eps),                                     // payout ratio
                percent(netIncome, equity),                                       // ROE
                percent(netIncome, assets),                                       // ROA
                divide(liabilities, equity, RATIO_SCALE),                         // debt-to-equity
                divide(currentAssets, currentLiabilities, RATIO_SCALE)            // current ratio
        );
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal ratio = divide(numerator, denominator, PERCENT_SCALE + 2);
        return ratio == null ? null : ratio.multiply(HUNDRED).setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return null;
        }
        return a.multiply(b);
    }
}
