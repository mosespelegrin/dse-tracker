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
}
