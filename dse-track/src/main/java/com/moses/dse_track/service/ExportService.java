package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.Dividend;
import com.moses.dse_track.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

// Exports transaction/dividend history as CSV or PDF — the inverse of the
// CSV order-import feature.
@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionService transactionService;
    private final DividendService dividendService;

    public byte[] exportTransactionsCsv(Long userId) {
        String[] headers = {"Date", "Ticker", "Company", "Type", "Shares", "Total Paid", "Sell Price", "Notes"};
        List<String[]> rows = transactionService.getHistory(userId).stream()
                .map(this::transactionRow)
                .toList();
        return renderCsv(headers, rows);
    }

    public byte[] exportTransactionsPdf(Long userId) {
        String[] headers = {"Date", "Ticker", "Type", "Shares", "Total Paid", "Sell Price"};
        List<String[]> rows = transactionService.getHistory(userId).stream()
                .map(t -> new String[]{
                        String.valueOf(t.getDate()),
                        t.getStock().getTicker(),
                        t.getType().name(),
                        String.valueOf(t.getShares()),
                        String.valueOf(t.getTotalPaid()),
                        t.getSellPrice() == null ? "-" : String.valueOf(t.getSellPrice())
                })
                .toList();
        return renderPdf("Transaction History", headers, rows);
    }

    public byte[] exportDividendsCsv(Long userId) {
        String[] headers = {"Payment Date", "Ticker", "Company", "Amount/Share", "Shares", "Total", "Notes"};
        List<String[]> rows = dividendService.getHistory(userId).stream()
                .map(this::dividendRow)
                .toList();
        return renderCsv(headers, rows);
    }

    public byte[] exportDividendsPdf(Long userId) {
        String[] headers = {"Payment Date", "Ticker", "Amount/Share", "Shares", "Total"};
        List<String[]> rows = dividendService.getHistory(userId).stream()
                .map(d -> new String[]{
                        String.valueOf(d.getPaymentDate()),
                        d.getStock().getTicker(),
                        String.valueOf(d.getAmountPerShare()),
                        String.valueOf(d.getShares()),
                        String.valueOf(d.getTotalAmount())
                })
                .toList();
        return renderPdf("Dividend History", headers, rows);
    }

    private String[] transactionRow(Transaction t) {
        return new String[]{
                String.valueOf(t.getDate()),
                t.getStock().getTicker(),
                t.getStock().getCompanyName(),
                t.getType().name(),
                String.valueOf(t.getShares()),
                String.valueOf(t.getTotalPaid()),
                t.getSellPrice() == null ? "" : String.valueOf(t.getSellPrice()),
                t.getNotes() == null ? "" : t.getNotes()
        };
    }

    private String[] dividendRow(Dividend d) {
        return new String[]{
                String.valueOf(d.getPaymentDate()),
                d.getStock().getTicker(),
                d.getStock().getCompanyName(),
                String.valueOf(d.getAmountPerShare()),
                String.valueOf(d.getShares()),
                String.valueOf(d.getTotalAmount()),
                d.getNotes() == null ? "" : d.getNotes()
        };
    }

    private byte[] renderCsv(String[] headers, List<String[]> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build());

            for (String[] row : rows) {
                String[] safeRow = new String[row.length];
                for (int i = 0; i < row.length; i++) {
                    safeRow[i] = neutralizeFormula(row[i]);
                }
                printer.printRecord((Object[]) safeRow);
            }
            printer.flush();
            printer.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Failed to generate CSV export");
        }
    }

    // "CSV injection": a cell starting with =, +, -, or @ (e.g. from a free-text
    // "notes" field) is interpreted as a formula by Excel/Sheets when the file is
    // opened, which can trigger code execution or data exfiltration. Prefixing
    // with a leading apostrophe forces it to be read back as plain text instead.
    private String neutralizeFormula(String cell) {
        if (cell == null || cell.isEmpty()) return cell;
        char first = cell.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') {
            return "'" + cell;
        }
        return cell;
    }

    private byte[] renderPdf(String title, String[] headers, List<String[]> rows) {
        float margin = 50f;
        float rowHeight = 16f;
        float pageWidth = PDRectangle.A4.getWidth();
        float pageHeight = PDRectangle.A4.getHeight();
        float[] colWidths = evenWidths(headers.length, pageWidth - 2 * margin);

        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float y = pageHeight - margin;
            cs.beginText();
            cs.setFont(bold, 14);
            cs.newLineAtOffset(margin, y);
            cs.showText(sanitize(title));
            cs.endText();
            y -= rowHeight * 2;

            drawRow(cs, headers, colWidths, margin, y, bold, 10);
            y -= rowHeight;

            for (String[] row : rows) {
                if (y < margin + rowHeight) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - margin;
                }
                drawRow(cs, row, colWidths, margin, y, regular, 9);
                y -= rowHeight;
            }
            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Failed to generate PDF export");
        }
    }

    private void drawRow(PDPageContentStream cs, String[] cells, float[] colWidths, float startX, float y,
                          PDFont font, float fontSize) throws IOException {
        float x = startX;
        for (int i = 0; i < cells.length; i++) {
            String text = sanitize(cells[i]);
            if (text.length() > 22) {
                text = text.substring(0, 22) + "...";
            }
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
            x += colWidths[i];
        }
    }

    private float[] evenWidths(int count, float totalWidth) {
        float[] widths = new float[count];
        Arrays.fill(widths, totalWidth / count);
        return widths;
    }

    // Standard14 PDF fonts only support WinAnsiEncoding (roughly Latin-1) —
    // strip anything outside that range so a stray emoji/unicode char in a
    // user's free-text "notes" field can't crash PDF generation.
    private String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(c < 32 || c > 255 ? '?' : c);
        }
        return sb.toString();
    }
}
