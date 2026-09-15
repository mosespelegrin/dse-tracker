package com.moses.dse_track.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderImportResponse {

    private int totalRows;
    private int matched;
    private int mismatched;
    private int notFound;

    private List<OrderMatchResult> rows;

    // Transactions logged in the app, within the file's date range, that the
    // file never mentioned — i.e. possibly recorded here but missing from DSE
    private List<TransactionResponse> unmatchedAppTransactions;
}
