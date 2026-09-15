package com.moses.dse_track.service;

import com.moses.dse_track.model.Holding;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.repository.HoldingRepository;
import com.moses.dse_track.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    // Get all holdings for dashboard — no price needed
    public List<Holding> getHoldings(Long userId) {
        return holdingRepository.findByUserId(userId);
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