package com.moses.dse_track.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

// One row of the uploaded DSE order-export file, and how it reconciled
// against the user's transactions already logged in DSE Track.
public record OrderMatchResult(
        int row,
        String ticker,
        String type,
        Integer shares,
        BigDecimal amount,
        LocalDate date,
        String status,          // MATCHED, SHARES_MISMATCH, AMOUNT_MISMATCH, NOT_FOUND_IN_APP, UNKNOWN_TICKER, UNREADABLE_ROW
        String message,
        Long matchedTransactionId
) {}
