package com.moses.dse_track.dto.response;

import com.moses.dse_track.model.Dividend;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DividendResponse {

    private Long id;
    private String ticker;
    private String companyName;
    private BigDecimal amountPerShare;
    private Integer shares;
    private BigDecimal totalAmount;
    private LocalDate paymentDate;
    private String notes;

    public DividendResponse(Dividend dividend) {
        this.id = dividend.getId();
        this.ticker = dividend.getStock().getTicker();
        this.companyName = dividend.getStock().getCompanyName();
        this.amountPerShare = dividend.getAmountPerShare();
        this.shares = dividend.getShares();
        this.totalAmount = dividend.getTotalAmount();
        this.paymentDate = dividend.getPaymentDate();
        this.notes = dividend.getNotes();
    }
}
