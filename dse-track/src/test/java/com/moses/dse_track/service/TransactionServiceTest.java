package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Holding;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.HoldingRepository;
import com.moses.dse_track.repository.StockRepository;
import com.moses.dse_track.repository.TransactionRepository;
import com.moses.dse_track.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private StockRepository stockRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Stock stock;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Moses").email("moses@example.com").build();
        stock = Stock.builder().id(10L).ticker("CRDB").companyName("CRDB Bank PLC").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockRepository.findById(10L)).thenReturn(Optional.of(stock));
    }

    @Test
    void buyCreatesANewHoldingWhenNoneExists() {
        when(holdingRepository.findByUserIdAndStockIdForUpdate(1L, 10L)).thenReturn(Optional.empty());

        transactionService.buy(1L, 10L, 100, new BigDecimal("5000"), LocalDate.now(), "first buy");

        verify(holdingRepository).save(argThat(h ->
                h.getShares() == 100 && h.getTotalPaid().compareTo(new BigDecimal("5000")) == 0));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void buyAccumulatesAnExistingHolding() {
        Holding existing = Holding.builder().id(5L).user(user).stock(stock)
                .shares(50).totalPaid(new BigDecimal("2000")).build();
        when(holdingRepository.findByUserIdAndStockIdForUpdate(1L, 10L)).thenReturn(Optional.of(existing));

        transactionService.buy(1L, 10L, 25, new BigDecimal("1250"), LocalDate.now(), null);

        verify(holdingRepository).save(argThat(h ->
                h.getShares() == 75 && h.getTotalPaid().compareTo(new BigDecimal("3250")) == 0));
    }

    @Test
    void sellReducesHoldingAndRecordsCostBasis() {
        Holding existing = Holding.builder().id(5L).user(user).stock(stock)
                .shares(100).totalPaid(new BigDecimal("1000")).build(); // cost per share = 10
        when(holdingRepository.findByUserIdAndStockIdForUpdate(1L, 10L)).thenReturn(Optional.of(existing));

        Transaction result = transactionService.sell(
                1L, 10L, 40, new BigDecimal("600"), new BigDecimal("15"), LocalDate.now(), null);

        // 40 shares sold at a cost basis of 10/share = 400
        assertThat(result.getCostBasis()).isEqualByComparingTo("400.00");
        assertThat(result.getTotalPaid()).isEqualByComparingTo("600"); // proceeds received

        verify(holdingRepository).save(argThat(h ->
                h.getShares() == 60 && h.getTotalPaid().compareTo(new BigDecimal("600")) == 0));
        verify(holdingRepository, never()).delete(any());
    }

    @Test
    void sellingEverythingDeletesTheHolding() {
        Holding existing = Holding.builder().id(5L).user(user).stock(stock)
                .shares(40).totalPaid(new BigDecimal("400")).build();
        when(holdingRepository.findByUserIdAndStockIdForUpdate(1L, 10L)).thenReturn(Optional.of(existing));

        transactionService.sell(1L, 10L, 40, new BigDecimal("800"), new BigDecimal("20"), LocalDate.now(), null);

        verify(holdingRepository).delete(existing);
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void sellingMoreSharesThanOwnedIsRejected() {
        Holding existing = Holding.builder().id(5L).user(user).stock(stock)
                .shares(10).totalPaid(new BigDecimal("100")).build();
        when(holdingRepository.findByUserIdAndStockIdForUpdate(1L, 10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                transactionService.sell(1L, 10L, 50, new BigDecimal("500"), new BigDecimal("10"), LocalDate.now(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not enough shares");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void sellingAStockNeverOwnedIsRejected() {
        when(holdingRepository.findByUserIdAndStockIdForUpdate(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transactionService.sell(1L, 10L, 1, new BigDecimal("10"), new BigDecimal("10"), LocalDate.now(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("don't own");
    }
}
