package com.moses.dse_track.config;

import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.StockRepository;
import com.moses.dse_track.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Default admin account — the only account allowed to edit shared/global
    // fundamentals data (see SecurityConfig). Its password is intentionally
    // the same as its email per an explicit one-off request; that's a weak
    // credential and should be changed via /auth/forgot-password right after
    // first login rather than left as the standing admin password.
    private static final String ADMIN_EMAIL = "mosespelegrin2002@gmail.com";

    @Override
    public void run(String... args) {
        seedAdmin();
        seedStocks();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin user already seeded — skipping");
            return;
        }

        User admin = User.builder()
                .name("Moses Pelegrin")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_EMAIL))
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mustChangePassword(true)
                .build();
        userRepository.save(admin);
        log.warn("Seeded default admin user {} with a weak default password (same as its " +
                "email) — every endpoint except PUT /auth/change-password is blocked for it " +
                "until that password is changed", ADMIN_EMAIL);
    }

    private void seedStocks() {
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