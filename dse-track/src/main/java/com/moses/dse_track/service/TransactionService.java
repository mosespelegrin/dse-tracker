package com.moses.dse_track.service;

import com.moses.dse_track.model.Holding;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.model.User;
import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.repository.HoldingRepository;
import com.moses.dse_track.repository.StockRepository;
import com.moses.dse_track.repository.TransactionRepository;
import com.moses.dse_track.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;


    // LOG A BUY TRANSACTION
    @Transactional

    public Transaction buy(Long userId, Long stockId, Integer shares, BigDecimal totalPaid, LocalDate date, String notes) {

        // Step 1: Load user and stock from DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("Stock not found"));

        // Step 2: Save the transaction record permanently
        Transaction transaction = Transaction.builder()
                .user(user)
                .stock(stock)
                .type(Transaction.TransactionType.BUY)
                .shares(shares)
                .totalPaid(totalPaid)
                .date(date)
                .notes(notes)
                .build();

        transactionRepository.save(transaction);

        // Step 3: Update holdings
        // Does Moses already own this stock? (locked — see repository method)
        Optional<Holding> existingHolding = holdingRepository.findByUserIdAndStockIdForUpdate(userId, stockId);

        if (existingHolding.isPresent()) {
            // Already owns this stock — add to existing holding
            Holding holding = existingHolding.get();
            holding.setShares(holding.getShares() + shares);
            holding.setTotalPaid(holding.getTotalPaid().add(totalPaid));
            holdingRepository.save(holding);
        } else {
            // First time buying this stock — create new holding
            Holding newHolding = Holding.builder()
                    .user(user)
                    .stock(stock)
                    .shares(shares)
                    .totalPaid(totalPaid)
                    .build();
            holdingRepository.save(newHolding);
        }

        return transaction;
    }

    // LOG A SELL TRANSACTION
    @Transactional

    public Transaction sell(Long userId, Long stockId, Integer shares, BigDecimal totalReceived, BigDecimal sellPrice, LocalDate date, String notes) {

        // Step 1: Load user and stock
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("Stock not found"));

        // Step 2: Check Moses actually owns enough shares (locked — see repository method)
        Holding holding = holdingRepository.findByUserIdAndStockIdForUpdate(userId, stockId)
                .orElseThrow(() -> new BusinessException("You don't own this stock"));

        if (holding.getShares() < shares) {
            throw new BusinessException("Not enough shares. You own " + holding.getShares());
        }

        // Step 3: Cost basis of the shares being sold — computed before the
        // holding is mutated below, using its current weighted-average cost.
        // cost per share = total_paid / current_shares
        BigDecimal costPerShare = holding.getTotalPaid()
                .divide(BigDecimal.valueOf(holding.getShares()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal costOfSoldShares = costPerShare.multiply(BigDecimal.valueOf(shares));

        // Step 4: Save the transaction
        Transaction transaction = Transaction.builder()
                .user(user)
                .stock(stock)
                .type(Transaction.TransactionType.SELL)
                .shares(shares)
                .totalPaid(totalReceived)  // total money received
                .sellPrice(sellPrice)      // price per share they sold at
                .costBasis(costOfSoldShares)
                .date(date)
                .notes(notes)
                .build();

        transactionRepository.save(transaction);

        // Step 5: Update holding
        int remainingShares = holding.getShares() - shares;

        if (remainingShares == 0) {
            // Sold everything — delete the holding entirely
            holdingRepository.delete(holding);
        } else {
            // Partial sell — reduce shares and total paid proportionally
            holding.setShares(remainingShares);
            holding.setTotalPaid(holding.getTotalPaid().subtract(costOfSoldShares));
            holdingRepository.save(holding);
        }

        return transaction;
    }

    // GET TRANSACTION HISTORY

    public List<Transaction> getHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    public Page<Transaction> getHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId, pageable);
    }
}