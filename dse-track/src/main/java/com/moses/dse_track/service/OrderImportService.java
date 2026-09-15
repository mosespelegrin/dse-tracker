package com.moses.dse_track.service;

import com.moses.dse_track.dto.response.OrderImportResponse;
import com.moses.dse_track.dto.response.OrderMatchResult;
import com.moses.dse_track.dto.response.TransactionResponse;
import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Stock;
import com.moses.dse_track.model.Transaction;
import com.moses.dse_track.repository.StockRepository;
import com.moses.dse_track.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// Imports a CSV "order export" (e.g. from the DSE website or a broker
// contract note) and reconciles each row against the user's own logged
// transactions. There's no published, fixed column layout for these exports,
// so column detection is name-based and tolerant of common header variants.
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderImportService {

    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;

    private static final List<String> DATE_HEADERS =
            List.of("date", "trade date", "order date", "value date", "settlement date");
    private static final List<String> TICKER_HEADERS =
            List.of("ticker", "symbol", "security", "counter", "company");
    private static final List<String> TYPE_HEADERS =
            List.of("type", "side", "buy/sell", "order type", "b/s", "transaction type");
    private static final List<String> SHARES_HEADERS =
            List.of("shares", "quantity", "qty", "volume", "units");
    private static final List<String> PRICE_HEADERS =
            List.of("price", "unit price", "price per share", "rate");
    private static final List<String> TOTAL_HEADERS =
            List.of("total", "value", "amount", "total value", "consideration", "net amount");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    // Amount tolerance to absorb broker fees/commission baked into the export
    // but not into the app's recorded totalPaid (or vice versa)
    private static final BigDecimal AMOUNT_TOLERANCE_RATE = new BigDecimal("0.05");
    private static final BigDecimal MIN_AMOUNT_TOLERANCE = new BigDecimal("1");

    public OrderImportResponse importAndMatch(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }

        List<CSVRecord> records;
        Map<String, String> headerMap; // normalized (lowercase) header -> original header text
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            headerMap = new LinkedHashMap<>();
            for (String h : parser.getHeaderNames()) {
                headerMap.put(h.trim().toLowerCase(), h);
            }
            records = parser.getRecords();
        } catch (IOException e) {
            throw new BusinessException("Could not read the uploaded file — make sure it's a valid CSV export");
        }

        String dateCol = findColumn(headerMap, DATE_HEADERS);
        String tickerCol = findColumn(headerMap, TICKER_HEADERS);
        String typeCol = findColumn(headerMap, TYPE_HEADERS);
        String sharesCol = findColumn(headerMap, SHARES_HEADERS);
        String priceCol = findColumn(headerMap, PRICE_HEADERS);
        String totalCol = findColumn(headerMap, TOTAL_HEADERS);

        if (tickerCol == null || sharesCol == null || dateCol == null) {
            throw new BusinessException(
                    "Could not find ticker/shares/date columns in the uploaded file. Columns found: "
                            + headerMap.values());
        }

        // Fetch once up front instead of once per row — a CSV with a few
        // hundred rows would otherwise mean a few hundred extra DB round-trips.
        List<Transaction> userTransactions = transactionRepository.findByUserIdOrderByDateDesc(userId);
        Map<String, Stock> stocksByTicker = new HashMap<>();
        for (Stock s : stockRepository.findAll()) {
            stocksByTicker.put(s.getTicker().toUpperCase(), s);
        }

        List<OrderMatchResult> results = new ArrayList<>();
        Set<Long> matchedTransactionIds = new HashSet<>();
        LocalDate minDate = null;
        LocalDate maxDate = null;

        int rowNum = 1;
        for (CSVRecord record : records) {
            rowNum++;
            String ticker = "?";
            try {
                ticker = get(record, headerMap, tickerCol).toUpperCase();
                LocalDate date = parseDate(get(record, headerMap, dateCol));
                Integer shares = parseShares(get(record, headerMap, sharesCol));
                Transaction.TransactionType type = parseType(get(record, headerMap, typeCol));

                BigDecimal amount = null;
                if (totalCol != null) {
                    amount = parseAmount(get(record, headerMap, totalCol));
                }
                if (amount == null && priceCol != null && shares != null) {
                    BigDecimal price = parseAmount(get(record, headerMap, priceCol));
                    if (price != null) {
                        amount = price.multiply(BigDecimal.valueOf(shares));
                    }
                }

                if (minDate == null || date.isBefore(minDate)) minDate = date;
                if (maxDate == null || date.isAfter(maxDate)) maxDate = date;

                OrderMatchResult result = matchRow(rowNum, stocksByTicker, userTransactions, ticker, type, shares, date, amount);
                results.add(result);
                if (result.matchedTransactionId() != null) {
                    matchedTransactionIds.add(result.matchedTransactionId());
                }
            } catch (Exception e) {
                results.add(new OrderMatchResult(rowNum, ticker, null, null, null, null,
                        "UNREADABLE_ROW", "Could not parse this row: " + e.getMessage(), null));
            }
        }

        List<TransactionResponse> unmatchedAppTransactions = new ArrayList<>();
        if (minDate != null) {
            LocalDate from = minDate;
            LocalDate to = maxDate;
            unmatchedAppTransactions = userTransactions.stream()
                    .filter(t -> !t.getDate().isBefore(from) && !t.getDate().isAfter(to))
                    .filter(t -> !matchedTransactionIds.contains(t.getId()))
                    .map(TransactionResponse::new)
                    .toList();
        }

        int matched = (int) results.stream().filter(r -> "MATCHED".equals(r.status())).count();
        int mismatched = (int) results.stream()
                .filter(r -> "SHARES_MISMATCH".equals(r.status()) || "AMOUNT_MISMATCH".equals(r.status()))
                .count();
        int notFound = (int) results.stream()
                .filter(r -> "NOT_FOUND_IN_APP".equals(r.status()) || "UNKNOWN_TICKER".equals(r.status()))
                .count();

        return OrderImportResponse.builder()
                .totalRows(results.size())
                .matched(matched)
                .mismatched(mismatched)
                .notFound(notFound)
                .rows(results)
                .unmatchedAppTransactions(unmatchedAppTransactions)
                .build();
    }

    private OrderMatchResult matchRow(int rowNum, Map<String, Stock> stocksByTicker, List<Transaction> userTransactions,
                                       String ticker, Transaction.TransactionType type,
                                       Integer shares, LocalDate date, BigDecimal amount) {

        String typeName = type == null ? null : type.name();

        Stock stock = stocksByTicker.get(ticker);
        if (stock == null) {
            return new OrderMatchResult(rowNum, ticker, typeName, shares, amount, date,
                    "UNKNOWN_TICKER", "No stock with ticker '" + ticker + "' exists in DSE Track", null);
        }

        List<Transaction> candidates = userTransactions.stream()
                .filter(t -> t.getStock().getId().equals(stock.getId()))
                .filter(t -> t.getDate().equals(date))
                .filter(t -> type == null || t.getType() == type)
                .toList();

        if (candidates.isEmpty()) {
            return new OrderMatchResult(rowNum, ticker, typeName, shares, amount, date,
                    "NOT_FOUND_IN_APP", "This order wasn't found among your logged transactions", null);
        }

        Optional<Transaction> sharesMatch = candidates.stream()
                .filter(t -> t.getShares().equals(shares))
                .findFirst();

        if (sharesMatch.isEmpty()) {
            Transaction closest = candidates.get(0);
            return new OrderMatchResult(rowNum, ticker, typeName, shares, amount, date,
                    "SHARES_MISMATCH",
                    "Found a transaction on this date but share count differs (app: "
                            + closest.getShares() + ", file: " + shares + ")",
                    closest.getId());
        }

        Transaction match = sharesMatch.get();
        if (amount != null) {
            BigDecimal recorded = match.getTotalPaid();
            BigDecimal diff = amount.subtract(recorded).abs();
            BigDecimal threshold = recorded.multiply(AMOUNT_TOLERANCE_RATE).abs().max(MIN_AMOUNT_TOLERANCE);
            if (diff.compareTo(threshold) > 0) {
                return new OrderMatchResult(rowNum, ticker, typeName, shares, amount, date,
                        "AMOUNT_MISMATCH",
                        "Shares match but amount differs beyond tolerance (app: " + recorded + ", file: " + amount + ")",
                        match.getId());
            }
        }

        return new OrderMatchResult(rowNum, ticker, typeName, shares, amount, date,
                "MATCHED", "Matches your logged transaction", match.getId());
    }

    private String findColumn(Map<String, String> headerMap, List<String> candidates) {
        for (String candidate : candidates) {
            if (headerMap.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String get(CSVRecord record, Map<String, String> headerMap, String normalizedCol) {
        if (normalizedCol == null) return "";
        String original = headerMap.get(normalizedCol);
        if (original == null || !record.isMapped(original)) return "";
        String value = record.get(original);
        return value == null ? "" : value.trim();
    }

    private LocalDate parseDate(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            throw new BusinessException("Missing date");
        }
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new BusinessException("Unrecognized date format: '" + raw + "'");
    }

    private Integer parseShares(String raw) {
        String cleaned = raw.replaceAll("[,\\s]", "");
        if (cleaned.isEmpty()) {
            throw new BusinessException("Missing share quantity");
        }
        double value = Double.parseDouble(cleaned);
        if (value != Math.floor(value)) {
            throw new BusinessException("Fractional share count not supported: '" + raw + "'");
        }
        return (int) value;
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[,\\s]", "");
        if (cleaned.isEmpty()) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Transaction.TransactionType parseType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim().toLowerCase();
        if (v.startsWith("b")) return Transaction.TransactionType.BUY;
        if (v.startsWith("s")) return Transaction.TransactionType.SELL;
        return null;
    }
}
