package com.moses.dse_track.repository;

import com.moses.dse_track.model.Dividend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DividendRepository extends JpaRepository<Dividend, Long> {

    List<Dividend> findByUserIdOrderByPaymentDateDesc(Long userId);

    Page<Dividend> findByUserIdOrderByPaymentDateDesc(Long userId, Pageable pageable);
}
