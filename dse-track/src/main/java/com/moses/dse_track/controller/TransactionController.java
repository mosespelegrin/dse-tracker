package com.moses.dse_track.controller;

import com.moses.dse_track.dto.request.TransactionRequest;
import com.moses.dse_track.dto.response.OrderImportResponse;
import com.moses.dse_track.dto.response.TransactionResponse;
import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.service.ExportService;
import com.moses.dse_track.service.OrderImportService;
import com.moses.dse_track.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final OrderImportService orderImportService;
    private final ExportService exportService;




    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
    @GetMapping
    //GET /transactions?id=1
    public ResponseEntity<List<TransactionResponse>> getHistory(){
        Long userId=getCurrentUserId();
        List<TransactionResponse> history=transactionService.getHistory(userId)
                .stream()
                .map(TransactionResponse ::new)
                .toList();
        return ResponseEntity.ok(history);


    }
    //POST /transactions/buy?userId=1
    @PostMapping("/buy") //user bought stocks
    public ResponseEntity<TransactionResponse> buy(@Valid @RequestBody TransactionRequest transactionRequest){
        Long userId=getCurrentUserId();

        if (transactionRequest.getTotalPaid() == null
                || transactionRequest.getTotalPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total paid is required and must be greater than zero");
        }

        Transaction transaction=transactionService.buy(
                userId,
                transactionRequest.getStockId(),
                transactionRequest.getShares(),
                transactionRequest.getTotalPaid(),
                transactionRequest.getDate(),
                transactionRequest.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponse(transaction));
    }
    //POST /transactions/sell
    @PostMapping("/sell")
    public ResponseEntity<TransactionResponse> sell(
            @RequestBody TransactionRequest request) {

        Long userId = getCurrentUserId();

        // Validate — sell needs sellPrice not totalPaid
        if (request.getSellPrice() == null) {
            throw new BusinessException("Sell price per share is required");
        }
        if (request.getShares() == null || request.getShares() <= 0) {
            throw new BusinessException("Shares must be greater than zero");
        }

        // System calculates total received
        BigDecimal totalReceived = request.getSellPrice()
                .multiply(BigDecimal.valueOf(request.getShares()));

        Transaction transaction = transactionService.sell(
                userId,
                request.getStockId(),
                request.getShares(),
                totalReceived,           // ← calculated by system
                request.getSellPrice(),  // ← store sell price too
                request.getDate(),
                request.getNotes()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TransactionResponse(transaction));
    }

    // POST /transactions/import — upload a DSE/broker order-export CSV and
    // reconcile it against the user's logged transactions
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderImportResponse> importOrders(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        OrderImportResponse response = orderImportService.importAndMatch(userId, file);
        return ResponseEntity.ok(response);
    }

    // GET /transactions/page?page=0&size=20 — paginated history.
    // (GET /transactions above stays a plain array — the dashboard already
    // consumes it that way — this is an additive alternative for large histories.)
    @GetMapping("/page")
    public ResponseEntity<Page<TransactionResponse>> getHistoryPaged(
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(transactionService.getHistory(userId, pageable).map(TransactionResponse::new));
    }

    // GET /transactions/export.csv
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv() {
        Long userId = getCurrentUserId();
        byte[] csv = exportService.exportTransactionsCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // GET /transactions/export.pdf
    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf() {
        Long userId = getCurrentUserId();
        byte[] pdf = exportService.exportTransactionsPdf(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
