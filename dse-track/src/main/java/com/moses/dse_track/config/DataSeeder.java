package com.moses.dse_track.config;

import com.moses.dse_track.model.Stock;
import com.moses.dse_track.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) {
        if (stockRepository.count() > 0) {
            log.info("Stocks already seeded — skipping");
            return;
        }

        List<Stock> stocks = List.of(
                Stock.builder().ticker("TPCC").companyName("Tanzania Portland Cement PLC").sector("Industry").build(),
                Stock.builder().ticker("NMG").companyName("Nation Media Group").sector("Media").build(),
                Stock.builder().ticker("KA").companyName("Kenya Airways").sector("Transport").build(),
                Stock.builder().ticker("JHL").companyName("Jubilee Holdings Limited").sector("Insurance").build(),
                Stock.builder().ticker("TCC").companyName("Tanzania Cigarette Company").sector("Consumer").build(),
                Stock.builder().ticker("MCB").companyName("Maendeleo Bank PLC").sector("Banking").build(),
                Stock.builder().ticker("EABL").companyName("East African Breweries Limited").sector("Consumer").build(),
                Stock.builder().ticker("TCCL").companyName("Tanga Cement Company").sector("Industry").build(),
                Stock.builder().ticker("TBL").companyName("Tanzania Breweries Limited").sector("Consumer").build(),
                Stock.builder().ticker("YETU").companyName("Yetu Microfinance Bank").sector("Banking").build(),
                Stock.builder().ticker("PAL").companyName("Precision Air Services").sector("Transport").build(),
                Stock.builder().ticker("NICO").companyName("NICO Holdings Limited").sector("Insurance").build(),
                Stock.builder().ticker("MBP").companyName("Mkombozi Commercial Bank").sector("Banking").build(),
                Stock.builder().ticker("SWALA").companyName("Swala Oil & Gas Tanzania").sector("Energy").build(),
                Stock.builder().ticker("MKCB").companyName("Mwalimu Commercial Bank").sector("Banking").build(),
                Stock.builder().ticker("VODA").companyName("Vodacom Tanzania PLC").sector("Telecom").build(),
                Stock.builder().ticker("NMB").companyName("NMB Bank PLC").sector("Banking").build(),
                Stock.builder().ticker("JATU").companyName("Jatu PLC").sector("Other").build(),
                Stock.builder().ticker("TOL").companyName("TOL Gases Limited").sector("Industry").build(),
                Stock.builder().ticker("DSE").companyName("Dar es Salaam Stock Exchange PLC").sector("Financial Services").build(),
                Stock.builder().ticker("CRDB").companyName("CRDB Bank PLC").sector("Banking").build(),
                Stock.builder().ticker("SWIS").companyName("Swissport Tanzania PLC").sector("Transport").build(),
                Stock.builder().ticker("DCB").companyName("DCB Commercial Bank PLC").sector("Banking").build(),
                Stock.builder().ticker("KCB").companyName("KCB Bank Tanzania").sector("Banking").build(),
                Stock.builder().ticker("USL").companyName("Uchumi Supermarkets").sector("Retail").build(),
                Stock.builder().ticker("TTP").companyName("Tanzania Tea Packers PLC").sector("Agriculture").build()
        );

        stockRepository.saveAll(stocks);
        log.info("Seeded {} DSE stocks successfully", stocks.size());
    }
}