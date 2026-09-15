package com.moses.dse_track.repository;

import com.moses.dse_track.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All of Moses's transactions newest first
    // JPA generates: SELECT * FROM transactions WHERE user_id = ? ORDER BY date DESC
    List<Transaction> findByUserIdOrderByDateDesc(Long userId);

    // Same, but paginated
    Page<Transaction> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);

    // All transactions for Moses on a specific stock
    List<Transaction> findByUserIdAndStockId(Long userId, Long stockId);
}