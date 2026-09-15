package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Dividend;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.DividendRepository;
import com.moses.dse_track.repository.StockRepository;
import com.moses.dse_track.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DividendService {

    private final DividendRepository dividendRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    // Record a dividend payment the user received (manual entry — DSE has no
    // public dividend feed, so this mirrors how buy/sell transactions are logged)
    public Dividend recordDividend(Long userId, Long stockId, BigDecimal amountPerShare,
                                    Integer shares, LocalDate paymentDate, String notes) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("Stock not found"));

        BigDecimal totalAmount = amountPerShare.multiply(BigDecimal.valueOf(shares));

        Dividend dividend = Dividend.builder()
                .user(user)
                .stock(stock)
                .amountPerShare(amountPerShare)
                .shares(shares)
                .totalAmount(totalAmount)
                .paymentDate(paymentDate)
                .notes(notes)
                .build();

        return dividendRepository.save(dividend);
    }

    public List<Dividend> getHistory(Long userId) {
        return dividendRepository.findByUserIdOrderByPaymentDateDesc(userId);
    }

    public Page<Dividend> getHistory(Long userId, Pageable pageable) {
        return dividendRepository.findByUserIdOrderByPaymentDateDesc(userId, pageable);
    }

    public BigDecimal getTotalDividends(Long userId) {
        return getHistory(userId).stream()
                .map(Dividend::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
