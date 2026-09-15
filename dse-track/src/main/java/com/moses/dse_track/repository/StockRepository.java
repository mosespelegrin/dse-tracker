package com.moses.dse_track.repository;

import com.moses.dse_track.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock,Long> {


    Optional<Stock> findByTicker(String ticker);

    // JPA generates: SELECT COUNT(*) > 0 FROM stocks WHERE ticker = ?
    boolean existsByTicker(String ticker);

}
