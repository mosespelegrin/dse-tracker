package com.moses.dse_track.controller;

import com.moses.dse_track.dto.request.StockFundamentalsRequest;
import com.moses.dse_track.dto.response.StockResponse;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.service.FundamentalsService;
import com.moses.dse_track.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final FundamentalsService fundamentalsService;

    // GET /stocks — returns all DSE stocks, each with its fundamental ratios
    // computed fresh from its current price. Used to populate dropdowns
    // and to show P/E, P/B, dividend yield, etc.
    @GetMapping
    public ResponseEntity<List<StockResponse>> getAllStocks() {

        List<StockResponse> stocks = stockService.getAllStocks()
                //convert into streams
                .stream()
                .map(stock -> new StockResponse(stock, fundamentalsService.compute(stock)))
                .toList();

        return ResponseEntity.ok(stocks);
    }

    // GET /stocks/{id} — single stock with fundamentals
    @GetMapping("/{id}")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long id) {
        Stock stock = stockService.getById(id);
        return ResponseEntity.ok(new StockResponse(stock, fundamentalsService.compute(stock)));
    }

    // PUT /stocks/{id}/fundamentals — set/update the raw financial inputs
    // (EPS, shares outstanding, equity, etc.) used to compute the ratios above.
    // There's no per-company fundamentals feed for DSE stocks, so this is
    // manual entry — same pattern as logging a dividend.
    @PutMapping("/{id}/fundamentals")
    public ResponseEntity<StockResponse> updateFundamentals(
            @PathVariable Long id,
            @Valid @RequestBody StockFundamentalsRequest request) {
        Stock stock = stockService.updateFundamentals(id, request);
        return ResponseEntity.ok(new StockResponse(stock, fundamentalsService.compute(stock)));
    }
}
