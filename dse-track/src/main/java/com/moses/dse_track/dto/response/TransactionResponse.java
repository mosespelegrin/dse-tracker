package com.moses.dse_track.dto.response;

import com.moses.dse_track.model.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class TransactionResponse {

    private Long id;
    private String ticker;
    private String companyName;
    private String type;
    private Integer shares;
    private BigDecimal totalPaid;       // total money paid/received
    private BigDecimal sellPrice;       // price per share (SELL only)
    private BigDecimal realizedGain;    // totalPaid - costBasis (SELL only, null if not available)
    private LocalDate date;
    private String notes;

    public TransactionResponse(Transaction transaction) {
        this.id = transaction.getId();
        this.ticker = transaction.getStock().getTicker();
        this.companyName = transaction.getStock().getCompanyName();
        this.type = transaction.getType().name();
        this.shares = transaction.getShares();
        this.totalPaid = transaction.getTotalPaid();
        this.sellPrice = transaction.getSellPrice();
        this.realizedGain = (transaction.getType() == Transaction.TransactionType.SELL && transaction.getCostBasis() != null)
                ? transaction.getTotalPaid().subtract(transaction.getCostBasis())
                : null;
        this.date = transaction.getDate();
        this.notes = transaction.getNotes();
    }
}