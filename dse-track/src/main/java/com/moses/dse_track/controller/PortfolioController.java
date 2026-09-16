package com.moses.dse_track.controller;

import com.moses.dse_track.dto.response.PortfolioResponse;
import com.moses.dse_track.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;


    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // GET /portfolio
    // Returns holdings and portfolio P&L calculated automatically from scraped DSE market prices
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }

    // GET /portfolio/sectors — current holdings grouped by sector
    @GetMapping("/sectors")
    public ResponseEntity<List<PortfolioService.SectorAllocation>> getSectorBreakdown() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(portfolioService.getSectorBreakdown(userId));
    }

    // POST /portfolio/calculate
    // Calculates portfolio P&L with optional price overrides (defaults to scraped DSE market prices)
    @PostMapping("/calculate")
    public ResponseEntity<PortfolioResponse> calculatePnL(@RequestBody(required = false) Map<Long, BigDecimal> currentPrices) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(portfolioService.calculatePortfolio(userId, currentPrices));
    }
}