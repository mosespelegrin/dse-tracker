package com.moses.dse_track.repository;

import com.moses.dse_track.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByEventTypeAndCreatedAtAfter(ActivityLog.EventType eventType, LocalDateTime after);
}
