package com.moses.dse_track.controller;

import com.moses.dse_track.dto.response.ActivityLogResponse;
import com.moses.dse_track.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Admin-only — see SecurityConfig, which gates the whole /admin/** path to
// ROLE_ADMIN.
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ActivityLogService activityLogService;

    // GET /admin/analytics — headline counts for the admin dashboard.
    @GetMapping("/analytics")
    public ResponseEntity<ActivityLogService.Analytics> getAnalytics() {
        return ResponseEntity.ok(activityLogService.getAnalytics());
    }

    // GET /admin/activity-logs?page=0&size=25 — the underlying audit trail
    // the analytics above are aggregated from, most recent first.
    @GetMapping("/activity-logs")
    public ResponseEntity<Page<ActivityLogResponse>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Page<ActivityLogResponse> logs = activityLogService
                .getRecentLogs(PageRequest.of(page, Math.min(size, 100)))
                .map(ActivityLogResponse::new);
        return ResponseEntity.ok(logs);
    }
}
