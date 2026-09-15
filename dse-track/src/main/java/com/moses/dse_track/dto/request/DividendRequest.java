package com.moses.dse_track.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DividendRequest {

    @NotNull(message = "Stock ID is required")
    private Long stockId;

    @NotNull(message = "Amount per share is required")
    @Positive(message = "Amount per share must be greater than zero")
    private BigDecimal amountPerShare;

    @NotNull(message = "Shares is required")
    @Positive(message = "Shares must be greater than zero")
    private Integer shares;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    private String notes; // optional
}
