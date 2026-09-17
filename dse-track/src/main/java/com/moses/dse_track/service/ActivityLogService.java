package com.moses.dse_track.service;

import com.moses.dse_track.model.ActivityLog;
import com.moses.dse_track.repository.ActivityLogRepository;
import com.moses.dse_track.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public record Analytics(
            long totalUsers,
            long newUsersLast7Days,
            long loginSuccessLast7Days,
            long loginFailedLast7Days,
            long passwordChangesLast7Days,
            long fundamentalsEditsLast7Days
    ) {}

    // Recording an audit event is secondary to the action it's attached to
    // (a login/registration/etc. must still succeed even if this fails) —
    // any failure here is swallowed and logged rather than propagated.
    public void record(ActivityLog.EventType eventType, Long userId, String email, String detail) {
        try {
            activityLogRepository.save(ActivityLog.builder()
                    .eventType(eventType)
                    .userId(userId)
                    .email(email)
                    .detail(detail)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record activity log [{}]", eventType, e);
        }
    }

    public Analytics getAnalytics() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        return new Analytics(
                userRepository.count(),
                activityLogRepository.countByEventTypeAndCreatedAtAfter(ActivityLog.EventType.REGISTER, sevenDaysAgo),
                activityLogRepository.countByEventTypeAndCreatedAtAfter(ActivityLog.EventType.LOGIN_SUCCESS, sevenDaysAgo),
                activityLogRepository.countByEventTypeAndCreatedAtAfter(ActivityLog.EventType.LOGIN_FAILED, sevenDaysAgo),
                activityLogRepository.countByEventTypeAndCreatedAtAfter(ActivityLog.EventType.PASSWORD_CHANGED, sevenDaysAgo)
                        + activityLogRepository.countByEventTypeAndCreatedAtAfter(ActivityLog.EventType.PASSWORD_RESET, sevenDaysAgo),
                activityLogRepository.countByEventTypeAndCreatedAtAfter(ActivityLog.EventType.FUNDAMENTALS_UPDATED, sevenDaysAgo)
        );
    }

    public Page<ActivityLog> getRecentLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
