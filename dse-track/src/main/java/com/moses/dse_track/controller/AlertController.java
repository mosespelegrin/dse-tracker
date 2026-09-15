package com.moses.dse_track.controller;

import com.moses.dse_track.dto.request.AlertRequest;
import com.moses.dse_track.dto.response.AlertResponse;
import com.moses.dse_track.model.Alert;
import com.moses.dse_track.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // POST /alerts
    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@Valid @RequestBody AlertRequest request) {

        Long userId = getCurrentUserId();

        Alert alert = alertService.createAlert(
                userId,
                request.getStockId(),
                request.getConditionType(),
                request.getTargetPrice()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AlertResponse(alert));
    }

    // GET /alerts
    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAlerts() {
        Long userId = getCurrentUserId();

        List<AlertResponse> alerts = alertService.getUserAlerts(userId)
                .stream()
                .map(AlertResponse::new)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    // DELETE /alerts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        alertService.deleteAlert(userId, id);
        return ResponseEntity.noContent().build();
    }
}