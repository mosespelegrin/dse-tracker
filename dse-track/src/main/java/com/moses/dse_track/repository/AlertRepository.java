package com.moses.dse_track.repository;

import com.moses.dse_track.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {


    List<Alert> findByUserId(Long userId);

    // Only alerts not yet triggered — used when user enters a new price

    List<Alert> findByStockIdAndTriggeredFalse(Long stockId);
}