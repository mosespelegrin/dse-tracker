package com.moses.dse_track.service;

import com.moses.dse_track.model.Stock;
import com.moses.dse_track.service.FundamentalsService.StockFundamentals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FundamentalsServiceTest {

    private final FundamentalsService service = new FundamentalsService();

    @Test
    void computesEveryRatioWhenAllInputsPresent() {
        Stock stock = Stock.builder()
                .currentPrice(new BigDecimal("1000"))
                .epsTtm(new BigDecimal("50"))
                .sharesOutstanding(1_000_000L)
                .netIncome(new BigDecimal("50000000"))
                .totalEquity(new BigDecimal("500000000"))
                .totalAssets(new BigDecimal("2000000000"))
                .totalLiabilities(new BigDecimal("1500000000"))
                .currentAssets(new BigDecimal("300000000"))
                .currentLiabilities(new BigDecimal("150000000"))
                .annualDividendPerShare(new BigDecimal("20"))
                .build();

        StockFundamentals f = service.compute(stock);

        assertThat(f.peRatio()).isEqualByComparingTo("20.0000");
        assertThat(f.earningsYieldPercent()).isEqualByComparingTo("5.00");
        assertThat(f.bookValuePerShare()).isEqualByComparingTo("500.0000");
        assertThat(f.pbRatio()).isEqualByComparingTo("2.0000");
        assertThat(f.marketCap()).isEqualByComparingTo("1000000000"); // 1000 price * 1,000,000 shares
        assertThat(f.dividendYieldPercent()).isEqualByComparingTo("2.00");
        assertThat(f.payoutRatioPercent()).isEqualByComparingTo("40.00");
        assertThat(f.roePercent()).isEqualByComparingTo("10.00");
        assertThat(f.roaPercent()).isEqualByComparingTo("2.50");
        assertThat(f.debtToEquity()).isEqualByComparingTo("3.0000");
        assertThat(f.currentRatio()).isEqualByComparingTo("2.0000");
    }

    @Test
    void ratiosAreNullRatherThanFabricatedWhenInputsAreMissing() {
        Stock stock = Stock.builder()
                .currentPrice(new BigDecimal("1000"))
                .build(); // no eps, equity, etc. entered yet

        StockFundamentals f = service.compute(stock);

        assertThat(f.peRatio()).isNull();
        assertThat(f.earningsYieldPercent()).isNull();
        assertThat(f.bookValuePerShare()).isNull();
        assertThat(f.pbRatio()).isNull();
        assertThat(f.marketCap()).isNull();
        assertThat(f.dividendYieldPercent()).isNull();
        assertThat(f.payoutRatioPercent()).isNull();
        assertThat(f.roePercent()).isNull();
        assertThat(f.roaPercent()).isNull();
        assertThat(f.debtToEquity()).isNull();
        assertThat(f.currentRatio()).isNull();
    }

    @Test
    void zeroOrNegativeDenominatorProducesNullNotAnException() {
        Stock stock = Stock.builder()
                .currentPrice(new BigDecimal("1000"))
                .epsTtm(BigDecimal.ZERO)
                .sharesOutstanding(0L)
                .totalEquity(new BigDecimal("500"))
                .build();

        StockFundamentals f = service.compute(stock);

        assertThat(f.peRatio()).isNull();          // eps is zero
        assertThat(f.bookValuePerShare()).isNull(); // shares outstanding is zero
    }
}
