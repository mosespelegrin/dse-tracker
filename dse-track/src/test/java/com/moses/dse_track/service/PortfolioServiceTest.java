package com.moses.dse_track.service;

import com.moses.dse_track.model.Holding;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.HoldingRepository;
import com.moses.dse_track.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private HoldingRepository holdingRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private final User user = User.builder().id(1L).name("Moses").email("m@example.com").build();

    @Test
    void calculatePnLComputesGainLossAndRoi() {
        Stock stock = Stock.builder().id(1L).ticker("CRDB").companyName("CRDB Bank").build();
        Holding holding = Holding.builder().id(1L).user(user).stock(stock)
                .shares(10).totalPaid(new BigDecimal("1000")).build();

        PortfolioService.HoldingResult result = portfolioService.calculatePnL(holding, new BigDecimal("120"));

        assertThat(result.currentValue()).isEqualByComparingTo("1200"); // 10 * 120
        assertThat(result.gainLoss()).isEqualByComparingTo("200");
        assertThat(result.roiPercent()).isEqualByComparingTo("20.0000");
    }

    @Test
    void calculatePnLDoesNotDivideByZeroWhenNothingWasPaid() {
        Stock stock = Stock.builder().id(1L).ticker("FREE").companyName("Free Shares Ltd").build();
        Holding holding = Holding.builder().id(1L).user(user).stock(stock)
                .shares(10).totalPaid(BigDecimal.ZERO).build();

        PortfolioService.HoldingResult result = portfolioService.calculatePnL(holding, new BigDecimal("50"));

        assertThat(result.roiPercent()).isEqualByComparingTo("0");
    }

    @Test
    void totalRealizedGainSumsOnlySellsWithACostBasis() {
        Transaction profitableSell = Transaction.builder()
                .type(Transaction.TransactionType.SELL)
                .totalPaid(new BigDecimal("500")).costBasis(new BigDecimal("400")).build(); // +100

        Transaction losingSell = Transaction.builder()
                .type(Transaction.TransactionType.SELL)
                .totalPaid(new BigDecimal("300")).costBasis(new BigDecimal("350")).build(); // -50

        Transaction sellWithNoCostBasis = Transaction.builder() // logged before costBasis existed
                .type(Transaction.TransactionType.SELL)
                .totalPaid(new BigDecimal("999")).costBasis(null).build();

        Transaction buy = Transaction.builder()
                .type(Transaction.TransactionType.BUY)
                .totalPaid(new BigDecimal("1000")).build();

        when(transactionRepository.findByUserIdOrderByDateDesc(1L))
                .thenReturn(List.of(profitableSell, losingSell, sellWithNoCostBasis, buy));

        BigDecimal totalRealized = portfolioService.getTotalRealizedGain(1L);

        assertThat(totalRealized).isEqualByComparingTo("50"); // 100 - 50, the other two excluded
    }

    @Test
    void sectorBreakdownGroupsAndComputesPercentages() {
        Stock bankStock = Stock.builder().id(1L).ticker("CRDB").companyName("CRDB Bank").sector("Banking").build();
        Stock teleStock = Stock.builder().id(2L).ticker("VODA").companyName("Vodacom").sector("Telecom").build();

        Holding banking = Holding.builder().id(1L).user(user).stock(bankStock)
                .shares(10).totalPaid(new BigDecimal("750")).build();
        Holding telecom = Holding.builder().id(2L).user(user).stock(teleStock)
                .shares(5).totalPaid(new BigDecimal("250")).build();

        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(banking, telecom));

        List<PortfolioService.SectorAllocation> breakdown = portfolioService.getSectorBreakdown(1L);

        assertThat(breakdown).hasSize(2);
        assertThat(breakdown.get(0).sector()).isEqualTo("Banking"); // larger invested amount sorts first
        assertThat(breakdown.get(0).percentOfPortfolio()).isEqualByComparingTo("75.00");
        assertThat(breakdown.get(1).sector()).isEqualTo("Telecom");
        assertThat(breakdown.get(1).percentOfPortfolio()).isEqualByComparingTo("25.00");
    }

    @Test
    void getPortfolioCalculatesPnLAutomaticallyFromScrapedPrices() {
        Stock stock = Stock.builder().id(1L).ticker("CRDB").companyName("CRDB Bank").currentPrice(new BigDecimal("120")).build();
        Holding holding = Holding.builder().id(10L).user(user).stock(stock)
                .shares(10).totalPaid(new BigDecimal("1000")).build();

        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(holding));
        when(transactionRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of());

        var response = portfolioService.getPortfolio(1L);

        assertThat(response.getTotalInvested()).isEqualByComparingTo("1000");
        assertThat(response.getTotalCurrentValue()).isEqualByComparingTo("1200");
        assertThat(response.getTotalGainLoss()).isEqualByComparingTo("200");
        assertThat(response.getOverallRoiPercent()).isEqualByComparingTo("20.0000");

        assertThat(response.getHoldings()).hasSize(1);
        var h = response.getHoldings().get(0);
        assertThat(h.getCurrentPrice()).isEqualByComparingTo("120");
        assertThat(h.getCurrentValue()).isEqualByComparingTo("1200");
        assertThat(h.getGainLoss()).isEqualByComparingTo("200");
        assertThat(h.getRoiPercent()).isEqualByComparingTo("20.0000");
    }

    @Test
    void calculatePortfolioAppliesPriceOverridesWhileDefaultingToScrapedPrices() {
        Stock stock1 = Stock.builder().id(1L).ticker("CRDB").companyName("CRDB Bank").currentPrice(new BigDecimal("100")).build();
        Stock stock2 = Stock.builder().id(2L).ticker("NMB").companyName("NMB Bank").currentPrice(new BigDecimal("200")).build();

        Holding holding1 = Holding.builder().id(10L).user(user).stock(stock1)
                .shares(10).totalPaid(new BigDecimal("1000")).build();
        Holding holding2 = Holding.builder().id(20L).user(user).stock(stock2)
                .shares(5).totalPaid(new BigDecimal("1000")).build();

        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(holding1, holding2));
        when(transactionRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of());

        // Override stock 1 to 150, leave stock 2 at scraped price (200)
        var response = portfolioService.calculatePortfolio(1L, Map.of(1L, new BigDecimal("150")));

        // Holding 1: 10 * 150 = 1500 (gain 500)
        // Holding 2: 5 * 200 = 1000 (gain 0)
        // Total currentValue = 2500, invested = 2000, gain = 500, roi = 25%
        assertThat(response.getTotalCurrentValue()).isEqualByComparingTo("2500");
        assertThat(response.getTotalGainLoss()).isEqualByComparingTo("500");
        assertThat(response.getOverallRoiPercent()).isEqualByComparingTo("25.0000");

        var h1 = response.getHoldings().stream().filter(h -> h.getStockId().equals(1L)).findFirst().orElseThrow();
        assertThat(h1.getCurrentPrice()).isEqualByComparingTo("150");
        assertThat(h1.getCurrentValue()).isEqualByComparingTo("1500");

        var h2 = response.getHoldings().stream().filter(h -> h.getStockId().equals(2L)).findFirst().orElseThrow();
        assertThat(h2.getCurrentPrice()).isEqualByComparingTo("200");
        assertThat(h2.getCurrentValue()).isEqualByComparingTo("1000");
    }

    @Test
    void getPortfolioHandlesHoldingWithNullPriceGracefully() {
        Stock stockWithoutPrice = Stock.builder().id(1L).ticker("NEW").companyName("New Company").currentPrice(null).build();
        Holding holding = Holding.builder().id(10L).user(user).stock(stockWithoutPrice)
                .shares(10).totalPaid(new BigDecimal("1000")).build();

        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(holding));
        when(transactionRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of());

        var response = portfolioService.getPortfolio(1L);

        assertThat(response.getTotalInvested()).isEqualByComparingTo("1000");
        assertThat(response.getTotalCurrentValue()).isNull();
        assertThat(response.getTotalGainLoss()).isNull();

        var h = response.getHoldings().get(0);
        assertThat(h.getCurrentPrice()).isNull();
        assertThat(h.getCurrentValue()).isNull();
        assertThat(h.getGainLoss()).isNull();
    }

    @Test
    void getPortfolioReturnsZerosForEmptyPortfolio() {
        when(holdingRepository.findByUserId(1L)).thenReturn(List.of());
        when(transactionRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of());

        var response = portfolioService.getPortfolio(1L);

        assertThat(response.getTotalInvested()).isEqualByComparingTo("0");
        assertThat(response.getTotalCurrentValue()).isEqualByComparingTo("0");
        assertThat(response.getTotalGainLoss()).isEqualByComparingTo("0");
        assertThat(response.getOverallRoiPercent()).isEqualByComparingTo("0");
        assertThat(response.getHoldings()).isEmpty();
    }
}
