package com.moses.dse_track.dto.response;

import com.moses.dse_track.model.Alert;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertResponse {

    private Long id;
    private String stockTicker;
    private String companyName;
    private String conditionType;
    private BigDecimal targetPrice;
    private Boolean triggered;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;

    public AlertResponse(Alert alert) {
        this.id            = alert.getId();
        this.stockTicker   = alert.getStock().getTicker();
        this.companyName   = alert.getStock().getCompanyName();
        this.conditionType = alert.getConditionType().name();
        this.targetPrice   = alert.getTargetPrice();
        this.triggered     = alert.getTriggered();
        this.triggeredAt   = alert.getTriggeredAt();
        this.createdAt     = alert.getCreatedAt();
    }
}