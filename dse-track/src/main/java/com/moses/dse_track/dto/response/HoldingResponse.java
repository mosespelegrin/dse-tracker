package com.moses.dse_track.dto.response;

import com.moses.dse_track.model.Holding;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldingResponse {

    private Long id;
    private Long stockId;
    private String ticker;
    private String companyName;
    private String sector;
    private Integer shares;
    private BigDecimal totalPaid;

    // Calculated fields — only populated when Moses enters current price
    private BigDecimal currentValue;
    private BigDecimal gainLoss;
    private BigDecimal roiPercent;

    // Constructor for portfolio view — no price yet
    public HoldingResponse(Holding holding) {
        this.id = holding.getId();
        this.stockId=holding.getStock().getId();
        this.ticker = holding.getStock().getTicker();
        this.companyName = holding.getStock().getCompanyName();
        this.sector = holding.getStock().getSector();
        this.shares = holding.getShares();
        this.totalPaid = holding.getTotalPaid();
    }

    // Constructor for P&L view — price entered, calculations done
    public HoldingResponse(Holding holding, BigDecimal currentValue,
                           BigDecimal gainLoss, BigDecimal roiPercent) {
        this(holding); // calls the constructor above first
        this.currentValue = currentValue;
        this.gainLoss = gainLoss;
        this.roiPercent = roiPercent;
    }
}
