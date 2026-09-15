package com.moses.dse_track.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRequest {

    @NotNull(message = "Stock ID is required")
    private Long stockId;

    @NotNull(message = "Condition is required")
    private String conditionType; // "ABOVE" or "BELOW"

    @NotNull(message = "Target price is required")
    @Positive(message = "Target price must be greater than zero")
    private BigDecimal targetPrice;
}