package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Alert;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.AlertRepository;
import com.moses.dse_track.repository.StockRepository;
import com.moses.dse_track.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final EmailService emailService;

    // ─────────────────────────────────────────
    // Create a new alert
    // ─────────────────────────────────────────
    public Alert createAlert(Long userId, Long stockId,
                             String conditionType, BigDecimal targetPrice) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("Stock not found"));

        Alert.AlertCondition condition;
        try {
            condition = Alert.AlertCondition.valueOf(conditionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Condition must be ABOVE or BELOW");
        }

        Alert alert = Alert.builder()
                .user(user)
                .stock(stock)
                .conditionType(condition)
                .targetPrice(targetPrice)
                .triggered(false)
                .build();

        return alertRepository.save(alert);
    }

    // ─────────────────────────────────────────
    // Get all alerts for a user
    // ─────────────────────────────────────────
    public List<Alert> getUserAlerts(Long userId) {
        return alertRepository.findByUserId(userId);
    }

    // ─────────────────────────────────────────
    // Delete an alert
    // ─────────────────────────────────────────
    public void deleteAlert(Long userId, Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("Alert not found"));

        if (!alert.getUser().getId().equals(userId)) {
            throw new BusinessException("You cannot delete another user's alert");
        }

        alertRepository.delete(alert);
    }

    // ─────────────────────────────────────────
    // Check all active alerts for a stock against a new price
    // Called automatically after DSE price scraper updates a stock
    // ─────────────────────────────────────────
    // @Transactional keeps the Hibernate session open for the whole loop so
    // alert.getUser() (LAZY) can be read below to send the notification email.
    @Transactional
    public void checkAlertsForStock(Stock stock, BigDecimal newPrice) {
        List<Alert> activeAlerts = alertRepository
                .findByStockIdAndTriggeredFalse(stock.getId());

        for (Alert alert : activeAlerts) {
            boolean shouldTrigger = false;

            if (alert.getConditionType() == Alert.AlertCondition.ABOVE
                    && newPrice.compareTo(alert.getTargetPrice()) >= 0) {
                shouldTrigger = true;
            }

            if (alert.getConditionType() == Alert.AlertCondition.BELOW
                    && newPrice.compareTo(alert.getTargetPrice()) <= 0) {
                shouldTrigger = true;
            }

            if (shouldTrigger) {
                alert.setTriggered(true);
                alert.setTriggeredAt(LocalDateTime.now());
                alertRepository.save(alert);
                log.info("Alert triggered: {} {} {} (price now {})",
                        stock.getTicker(),
                        alert.getConditionType(),
                        alert.getTargetPrice(),
                        newPrice);

                emailService.sendAlertTriggeredEmail(
                        alert.getUser().getEmail(),
                        alert.getUser().getName(),
                        stock.getTicker(),
                        stock.getCompanyName(),
                        alert.getConditionType().name(),
                        alert.getTargetPrice(),
                        newPrice
                );
            }
        }
    }
}