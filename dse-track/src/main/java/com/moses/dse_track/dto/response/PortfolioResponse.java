package com.moses.dse_track.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private List<HoldingResponse> holdings;
    private BigDecimal totalInvested;

    // Only present when Moses calculates P&L
    private BigDecimal totalCurrentValue;
    private BigDecimal totalGainLoss;      // unrealized — current holdings only
    private BigDecimal overallRoiPercent;

    // Realized — from past sells, independent of any price submitted here
    private BigDecimal totalRealizedGain;
}