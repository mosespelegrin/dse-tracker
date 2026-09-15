package com.moses.dse_track.controller;

import com.moses.dse_track.dto.response.HoldingResponse;
import com.moses.dse_track.dto.response.PortfolioResponse;
import com.moses.dse_track.model.Holding;
import com.moses.dse_track.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

    // GET /portfolio?userId=1
    // Returns holdings with no prices — just what Moses owns
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(){
        Long userId=getCurrentUserId();

        List<Holding> holdings = portfolioService.getHoldings(userId);

        List<HoldingResponse> holdingResponses = holdings.stream()
                .map(HoldingResponse::new)
                .toList();

        BigDecimal totalInvested = holdings.stream()
                .map(Holding::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PortfolioResponse response = PortfolioResponse.builder()
                .holdings(holdingResponses)
                .totalInvested(totalInvested)
                .totalRealizedGain(portfolioService.getTotalRealizedGain(userId))
                .build();

        return ResponseEntity.ok(response);
    }

    // GET /portfolio/sectors — current holdings grouped by sector
    @GetMapping("/sectors")
    public ResponseEntity<List<PortfolioService.SectorAllocation>> getSectorBreakdown() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(portfolioService.getSectorBreakdown(userId));
    }

    // POST /portfolio/calculate
    // Moses submits current prices → system calculates P&L
    // Body: { "1": 720.00, "2": 410.00 }  (stockId: currentPrice)
    @PostMapping("/calculate")
    public ResponseEntity<PortfolioResponse> calculatePnL(@RequestBody Map<Long, BigDecimal> currentPrices) {
        Long userId =getCurrentUserId();
        List<Holding> holdings = portfolioService.getHoldings(userId);

        List<HoldingResponse> results = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            totalInvested = totalInvested.add(holding.getTotalPaid());

            BigDecimal currentPrice = currentPrices.get(holding.getStock().getId());

            if (currentPrice != null) {
                // Price was entered for this stock — calculate P&L
                PortfolioService.HoldingResult result =
                        portfolioService.calculatePnL(holding, currentPrice);

                results.add(new HoldingResponse(
                        holding,
                        result.currentValue(),
                        result.gainLoss(),
                        result.roiPercent()
                ));

                totalCurrentValue = totalCurrentValue.add(result.currentValue());

            } else {
                // No price entered — show holding without P&L
                results.add(new HoldingResponse(holding));
            }
        }

        BigDecimal totalGainLoss = totalCurrentValue.subtract(totalInvested);
        BigDecimal overallRoi = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalGainLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        PortfolioResponse response = PortfolioResponse.builder()
                .holdings(results)
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalGainLoss(totalGainLoss)
                .overallRoiPercent(overallRoi)
                .totalRealizedGain(portfolioService.getTotalRealizedGain(userId))
                .build();

        return ResponseEntity.ok(response);
    }
}