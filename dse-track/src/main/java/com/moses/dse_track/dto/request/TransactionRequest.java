package com.moses.dse_track.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(message = "Stock ID is required")
    private Long stockId;

    @NotNull(message = "Shares is required")
    @Positive(message = "Shares must be greater than zero")
    private Integer shares;


    private BigDecimal totalPaid;
    //for selling

    private BigDecimal sellPrice;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String notes; // optional
}