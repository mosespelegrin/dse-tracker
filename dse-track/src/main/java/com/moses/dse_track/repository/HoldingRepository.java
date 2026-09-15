package com.moses.dse_track.repository;

import com.moses.dse_track.model.Holding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    // Get all stocks user  currently owns

    List<Holding> findByUserId(Long userId);

    // Check if user already has a holding for a specific stock

    Optional<Holding> findByUserIdAndStockId(Long userId, Long stockId);

    // Same lookup, but takes a row lock so concurrent buy/sell calls for the
    // same user+stock can't read-then-write over each other (lost update)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holding h where h.user.id = :userId and h.stock.id = :stockId")
    Optional<Holding> findByUserIdAndStockIdForUpdate(@Param("userId") Long userId, @Param("stockId") Long stockId);
}