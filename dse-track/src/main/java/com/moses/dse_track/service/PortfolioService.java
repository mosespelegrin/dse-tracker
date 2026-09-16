package com.moses.dse_track.service;

import com.moses.dse_track.dto.response.HoldingResponse;
import com.moses.dse_track.dto.response.PortfolioResponse;
import com.moses.dse_track.model.Holding;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.repository.HoldingRepository;
import com.moses.dse_track.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    // Get all holdings for dashboard
    public List<Holding> getHoldings(Long userId) {
        return holdingRepository.findByUserId(userId);
    }

    // Get full portfolio for dashboard — loss and gain are calculated automatically
    // based on the scraped market data from DSE (holding.getStock().getCurrentPrice())
    public PortfolioResponse getPortfolio(Long userId) {
        return calculatePortfolio(userId, Collections.emptyMap());
    }

    // Calculates portfolio metrics using scraped DSE prices with optional price overrides
    public PortfolioResponse calculatePortfolio(Long userId, Map<Long, BigDecimal> priceOverrides) {
        List<Holding> holdings = getHoldings(userId);

        List<HoldingResponse> results = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalInvestedForPricedHoldings = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        int pricedHoldingsCount = 0;

        for (Holding holding : holdings) {
            totalInvested = totalInvested.add(holding.getTotalPaid());

            // Use override price if explicitly provided, otherwise default to scraped DSE market price
            BigDecimal price = (priceOverrides != null && priceOverrides.containsKey(holding.getStock().getId()))
                    ? priceOverrides.get(holding.getStock().getId())
                    : holding.getStock().getCurrentPrice();

            if (price != null) {
                HoldingResult result = calculatePnL(holding, price);
                results.add(new HoldingResponse(
                        holding,
                        price,
                        result.currentValue(),
                        result.gainLoss(),
                        result.roiPercent()
                ));
                totalCurrentValue = totalCurrentValue.add(result.currentValue());
                totalInvestedForPricedHoldings = totalInvestedForPricedHoldings.add(holding.getTotalPaid());
                pricedHoldingsCount++;
            } else {
                results.add(new HoldingResponse(holding));
            }
        }

        BigDecimal totalGainLoss = null;
        BigDecimal overallRoi = null;

        if (holdings.isEmpty()) {
            totalCurrentValue = BigDecimal.ZERO;
            totalGainLoss = BigDecimal.ZERO;
            overallRoi = BigDecimal.ZERO;
        } else if (pricedHoldingsCount > 0) {
            totalGainLoss = totalCurrentValue.subtract(totalInvestedForPricedHoldings);
            overallRoi = totalInvestedForPricedHoldings.compareTo(BigDecimal.ZERO) > 0
                    ? totalGainLoss.divide(totalInvestedForPricedHoldings, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
        }

        return PortfolioResponse.builder()
                .holdings(results)
                .totalInvested(totalInvested)
                .totalCurrentValue(pricedHoldingsCount > 0 || holdings.isEmpty() ? totalCurrentValue : null)
                .totalGainLoss(totalGainLoss)
                .overallRoiPercent(overallRoi)
                .totalRealizedGain(getTotalRealizedGain(userId))
                .build();
    }

    // Realized gains — from past SELLs, using the cost basis captured at sale
    // time (see TransactionService.sell). Sells recorded before that field
    // existed have no costBasis and are excluded, since it can't be
    // recovered after the holding they were sold from has since changed.
    public BigDecimal getTotalRealizedGain(Long userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId).stream()
                .filter(t -> t.getType() == Transaction.TransactionType.SELL && t.getCostBasis() != null)
                .map(t -> t.getTotalPaid().subtract(t.getCostBasis()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Current holdings grouped by sector, with each sector's share of total invested capital
    public List<SectorAllocation> getSectorBreakdown(Long userId) {
        List<Holding> holdings = getHoldings(userId);

        BigDecimal total = holdings.stream()
                .map(Holding::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> investedBySector = holdings.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getStock().getSector() == null ? "Uncategorized" : h.getStock().getSector(),
                        Collectors.reducing(BigDecimal.ZERO, Holding::getTotalPaid, BigDecimal::add)
                ));

        return investedBySector.entrySet().stream()
                .map(e -> new SectorAllocation(
                        e.getKey(),
                        e.getValue(),
                        total.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO
                ))
                .sorted(Comparator.comparing(SectorAllocation::invested).reversed())
                .toList();
    }

    public record SectorAllocation(String sector, BigDecimal invested, BigDecimal percentOfPortfolio) {}

    // Calculate P&L for one holding given current price user entered
    public HoldingResult calculatePnL(Holding holding, BigDecimal currentPrice) {

        BigDecimal currentValue = currentPrice
                .multiply(BigDecimal.valueOf(holding.getShares()));

        BigDecimal gainLoss = currentValue.subtract(holding.getTotalPaid());

        BigDecimal roiPercent = holding.getTotalPaid().compareTo(BigDecimal.ZERO) > 0
                ? gainLoss.divide(holding.getTotalPaid(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new HoldingResult(
                holding.getStock().getTicker(),
                holding.getStock().getCompanyName(),
                holding.getShares(),
                holding.getTotalPaid(),
                currentValue,
                gainLoss,
                roiPercent
        );
    }

    // Simple record to hold calculation result — no DB table needed
    public record HoldingResult(
            String ticker,
            String companyName,
            Integer shares,
            BigDecimal totalPaid,
            BigDecimal currentValue,
            BigDecimal gainLoss,
            BigDecimal roiPercent
    ) {}
}