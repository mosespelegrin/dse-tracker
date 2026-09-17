package com.moses.dse_track.dto.response;

import com.moses.dse_track.model.ActivityLog;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ActivityLogResponse {

    Long id;
    ActivityLog.EventType eventType;
    Long userId;
    String email;
    String detail;
    LocalDateTime createdAt;

    public ActivityLogResponse(ActivityLog log) {
        this.id = log.getId();
        this.eventType = log.getEventType();
        this.userId = log.getUserId();
        this.email = log.getEmail();
        this.detail = log.getDetail();
        this.createdAt = log.getCreatedAt();
    }
}
