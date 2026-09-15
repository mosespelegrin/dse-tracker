package com.moses.dse_track.controller;

import com.moses.dse_track.dto.request.DividendRequest;
import com.moses.dse_track.dto.response.DividendResponse;
import com.moses.dse_track.model.Dividend;
import com.moses.dse_track.service.DividendService;
import com.moses.dse_track.service.ExportService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dividends")
@RequiredArgsConstructor
public class DividendController {

    private final DividendService dividendService;
    private final ExportService exportService;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // POST /dividends — log a dividend payment received
    @PostMapping
    public ResponseEntity<DividendResponse> recordDividend(@Valid @RequestBody DividendRequest request) {
        Long userId = getCurrentUserId();

        Dividend dividend = dividendService.recordDividend(
                userId,
                request.getStockId(),
                request.getAmountPerShare(),
                request.getShares(),
                request.getPaymentDate(),
                request.getNotes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new DividendResponse(dividend));
    }

    // GET /dividends — dividend history, newest first
    @GetMapping
    public ResponseEntity<List<DividendResponse>> getHistory() {
        Long userId = getCurrentUserId();

        List<DividendResponse> history = dividendService.getHistory(userId)
                .stream()
                .map(DividendResponse::new)
                .toList();

        return ResponseEntity.ok(history);
    }

    // GET /dividends/total — lifetime dividend income
    @GetMapping("/total")
    public ResponseEntity<Map<String, BigDecimal>> getTotal() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(Map.of("totalDividends", dividendService.getTotalDividends(userId)));
    }

    // GET /dividends/page?page=0&size=20 — paginated history
    @GetMapping("/page")
    public ResponseEntity<Page<DividendResponse>> getHistoryPaged(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(dividendService.getHistory(userId, pageable).map(DividendResponse::new));
    }

    // GET /dividends/export.csv
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv() {
        Long userId = getCurrentUserId();
        byte[] csv = exportService.exportDividendsCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dividends.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // GET /dividends/export.pdf
    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf() {
        Long userId = getCurrentUserId();
        byte[] pdf = exportService.exportDividendsPdf(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dividends.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
