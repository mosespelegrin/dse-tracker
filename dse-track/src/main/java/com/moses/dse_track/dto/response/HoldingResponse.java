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

    // Scraped market price from DSE (or override)
    private BigDecimal currentPrice;

    // Calculated fields based on currentPrice
    private BigDecimal currentValue;
    private BigDecimal gainLoss;
    private BigDecimal roiPercent;

    // Default constructor — extracts current price from holding's stock
    public HoldingResponse(Holding holding) {
        this.id = holding.getId();
        this.stockId = holding.getStock().getId();
        this.ticker = holding.getStock().getTicker();
        this.companyName = holding.getStock().getCompanyName();
        this.sector = holding.getStock().getSector();
        this.shares = holding.getShares();
        this.totalPaid = holding.getTotalPaid();
        this.currentPrice = holding.getStock().getCurrentPrice();
    }

    // Constructor with explicit price and calculated P&L
    public HoldingResponse(Holding holding, BigDecimal currentPrice, BigDecimal currentValue,
                           BigDecimal gainLoss, BigDecimal roiPercent) {
        this(holding);
        this.currentPrice = currentPrice;
        this.currentValue = currentValue;
        this.gainLoss = gainLoss;
        this.roiPercent = roiPercent;
    }

    // Constructor for backward compatibility
    public HoldingResponse(Holding holding, BigDecimal currentValue,
                           BigDecimal gainLoss, BigDecimal roiPercent) {
        this(holding, holding.getStock().getCurrentPrice(), currentValue, gainLoss, roiPercent);
    }
}
