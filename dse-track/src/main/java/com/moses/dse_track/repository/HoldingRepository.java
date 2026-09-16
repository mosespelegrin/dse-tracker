package com.moses.dse_track.repository;

import com.moses.dse_track.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
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
    // same user+stock can't read-then-write over each other (lost update).
    // Plain native "for update" — Hibernate's JPQL @Lock(PESSIMISTIC_WRITE) on this
    // dialect emits "for update of <alias>", which MariaDB's parser rejects (that
    // "OF <table>" form isn't MariaDB syntax), breaking every buy/sell with a 500.
    @Query(value = "select * from holdings where user_id = :userId and stock_id = :stockId for update", nativeQuery = true)
    Optional<Holding> findByUserIdAndStockIdForUpdate(@Param("userId") Long userId, @Param("stockId") Long stockId);
}