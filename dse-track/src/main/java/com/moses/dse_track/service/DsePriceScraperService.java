package com.moses.dse_track.service;

import com.moses.dse_track.dto.DsePriceDto;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DsePriceScraperService {

    private final StockRepository stockRepository;
    private final AlertService alertService;   // ← NEW — added this field
    private final RestTemplate restTemplate;
    private final FundamentalsService fundamentalsService;

    private static final String DSE_URL = "https://dse.co.tz/api/get/live/market/prices";

    @Scheduled(cron = "0 0 13 * * MON-FRI", zone = "Africa/Dar_es_Salaam")
    public void fetchAndUpdatePrices() {
        log.info("Starting scheduled DSE price fetch at {}", LocalDateTime.now());
        fetchPrices();
    }

    public int fetchPrices() {
        try {
            DsePriceDto response = restTemplate.getForObject(DSE_URL, DsePriceDto.class);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("DSE API returned no valid data");
                return 0;
            }

            int updated = 0;
            int skipped = 0;

            for (DsePriceDto.DseStock dseStock : response.getData()) {
                String ticker = dseStock.getTicker();
                if (ticker == null || ticker.trim().isEmpty()) {
                    skipped++;
                    continue;
                }

                Optional<Stock> stockOpt = stockRepository.findByTicker(ticker.trim());

                if (stockOpt.isPresent()) {
                    Stock stock = stockOpt.get();
                    stock.setCurrentPrice(dseStock.getPrice());
                    stock.setLastUpdated(LocalDateTime.now());
                    stockRepository.save(stock);

                    // ← NEW — check alerts right after price is saved
                    alertService.checkAlertsForStock(stock, dseStock.getPrice());

                    // Fundamental ratios are always computed fresh on every API
                    // read, so nothing needs to be persisted here — this just
                    // runs (and logs) the formulas immediately, as required,
                    // using the price that was just saved above.
                    FundamentalsService.StockFundamentals f = fundamentalsService.compute(stock);
                    log.info("Fundamentals for {} → P/E={}, P/B={}, Div Yield={}%, ROE={}%",
                            ticker, f.peRatio(), f.pbRatio(), f.dividendYieldPercent(), f.roePercent());

                    updated++;
                    log.info("Updated price for {} → TZS {}", ticker, dseStock.getPrice());
                } else {
                    skipped++;
                    log.debug("Stock not found in DB: {}", ticker);
                }
            }

            log.info("DSE price fetch completed. Updated: {}, Skipped: {}", updated, skipped);
            return updated;

        } catch (Exception e) {
            log.error("Failed to fetch DSE prices", e);
            return 0;
        }
    }
}