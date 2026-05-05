package com.levanto.flooring.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.levanto.flooring.config.CompanyProperties;
import com.levanto.flooring.entity.Expense;
import com.levanto.flooring.enums.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpensePdfService {

    private final CompanyProperties co;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final BaseColor NAVY = new BaseColor(15, 35, 56);
    private static final BaseColor NAVY2 = new BaseColor(26, 58, 92);
    private static final BaseColor GOLD = new BaseColor(200, 151, 42);
    private static final BaseColor LIGHT = new BaseColor(243, 246, 250);
    private static final BaseColor BORDER = new BaseColor(210, 218, 228);
    private static final BaseColor DARK = new BaseColor(25, 25, 35);
    private static final BaseColor MUTED = new BaseColor(100, 118, 140);
    private static final BaseColor CAT_BG = new BaseColor(232, 239, 248);

    // ── Category colours (distinct per category) ──────────────────────────────
    private static final Map<ExpenseCategory, BaseColor> CAT_COLOURS = new EnumMap<>(ExpenseCategory.class);
    static {
        CAT_COLOURS.put(ExpenseCategory.RENT, new BaseColor(220, 38, 38)); // red
        CAT_COLOURS.put(ExpenseCategory.MATERIAL, new BaseColor(37, 99, 235)); // blue
        CAT_COLOURS.put(ExpenseCategory.SALARY, new BaseColor(124, 58, 237)); // purple
        CAT_COLOURS.put(ExpenseCategory.UTILITY, new BaseColor(5, 150, 105)); // green
        CAT_COLOURS.put(ExpenseCategory.MISC, new BaseColor(180, 120, 20)); // amber
    }

    private Font f(int size, int style, BaseColor c) {
        try {
            BaseFont bf = (style == Font.BOLD)
                    ? BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false)
                    : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            return new Font(bf, size, style, c);
        } catch (Exception e) {
            return new Font(Font.FontFamily.HELVETICA, size, style, c);
        }
    }

    public byte[] generate(List<Expense> expenses, LocalDate from, LocalDate to,
            ExpenseCategory filterCategory) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(co));
            doc.open();

            // ── Document Header ───────────────────────────────────────────────
            addDocHeader(doc, from, to, filterCategory);

            // ── Summary row (totals per category) ────────────────────────────
            addSummaryTable(doc, expenses, filterCategory);

            doc.add(Chunk.NEWLINE);

            // ── Group expenses by category ────────────────────────────────────
            Map<ExpenseCategory, List<Expense>> grouped = expenses.stream()
                    .collect(Collectors.groupingBy(Expense::getCategory,
                            LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<ExpenseCategory, List<Expense>> entry : grouped.entrySet()) {
                addCategoryBlock(doc, entry.getKey(), entry.getValue());
                doc.add(new Paragraph(" "));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Expense PDF generation failed", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Top header with company info + report title ───────────────────────────
    private void addDocHeader(Document doc, LocalDate from, LocalDate to,
            ExpenseCategory filterCat) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[] { 3f, 1.6f });
        t.setSpacingAfter(14);

        // Left: company
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setBackgroundColor(NAVY);
        left.setPaddingLeft(16);
        left.setPaddingRight(16);
        left.setPaddingBottom(14);
        left.setPaddingTop(2);

        left.addElement(new Paragraph(co.getName(), f(22, Font.BOLD, BaseColor.WHITE)));
        left.addElement(new Paragraph(co.getTagline(), f(9, Font.NORMAL, BaseColor.WHITE)));
        left.addElement(new Paragraph(co.getAddress(), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));
        left.addElement(new Paragraph(co.getPhone() + "  |  " + co.getEmail(),
                f(8, Font.NORMAL, new BaseColor(170, 195, 225))));
        left.addElement(new Paragraph("GSTIN: " + co.getGstNumber(), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));

        t.addCell(left);

        // Right: report badge
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(GOLD);
        right.setPaddingLeft(16);
        right.setPaddingRight(16);
        right.setPaddingBottom(16);
        right.setPaddingTop(2);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph title = new Paragraph("EXPENSE REPORT", f(11, Font.BOLD, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        right.addElement(title);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Paragraph period = new Paragraph(from.format(fmt) + " – " + to.format(fmt),
                f(8, Font.NORMAL, NAVY));
        period.setAlignment(Element.ALIGN_CENTER);
        right.addElement(period);

        if (filterCat != null) {
            Paragraph cat = new Paragraph("Category: " + filterCat.name(), f(8, Font.BOLD, NAVY));
            cat.setAlignment(Element.ALIGN_CENTER);
            right.addElement(cat);
        }
        t.addCell(right);
        doc.add(t);
    }

    // ── Summary table: one row per category present ───────────────────────────
    private void addSummaryTable(Document doc, List<Expense> expenses,
            ExpenseCategory filterCat) throws DocumentException {

        Paragraph heading = new Paragraph("Summary", f(11, Font.BOLD, GOLD));
        heading.setSpacingAfter(6);
        doc.add(heading);

        // Group totals
        Map<ExpenseCategory, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense e : expenses) {
            totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }

        PdfPTable t = new PdfPTable(filterCat == null ? 3 : 2);
        t.setWidthPercentage(filterCat == null ? 70 : 50);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setSpacingAfter(10);

        // Header
        String[] hdrs = filterCat == null
                ? new String[] { "Category", "Count", "Amount ₹" }
                : new String[] { "Category", "Amount ₹" };
        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, f(8, Font.BOLD, BaseColor.WHITE)));
            c.setBackgroundColor(NAVY);
            c.setPadding(7);
            c.setBorderColor(NAVY2);
            t.addCell(c);
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        int grandCount = 0;
        boolean alt = false;

        for (Map.Entry<ExpenseCategory, BigDecimal> entry : totals.entrySet()) {
            long count = expenses.stream()
                    .filter(e -> e.getCategory() == entry.getKey()).count();
            BaseColor bg = alt ? LIGHT : BaseColor.WHITE;

            // Coloured category pill
            PdfPCell catCell = new PdfPCell();
            catCell.setBackgroundColor(bg);
            catCell.setPadding(6);
            catCell.setBorderColor(BORDER);
            BaseColor dot = CAT_COLOURS.getOrDefault(entry.getKey(), MUTED);
            Paragraph catP = new Paragraph();
            catP.add(new Chunk("● ", f(10, Font.BOLD, dot)));
            catP.add(new Chunk(entry.getKey().name(), f(9, Font.NORMAL, DARK)));
            catCell.addElement(catP);
            t.addCell(catCell);

            if (filterCat == null) {
                PdfPCell cntCell = new PdfPCell(new Phrase(String.valueOf(count), f(9, Font.NORMAL, DARK)));
                cntCell.setBackgroundColor(bg);
                cntCell.setPadding(6);
                cntCell.setBorderColor(BORDER);
                t.addCell(cntCell);
            }

            PdfPCell amtCell = new PdfPCell(new Phrase("₹ " + fmt(entry.getValue()), f(9, Font.BOLD, DARK)));
            amtCell.setBackgroundColor(bg);
            amtCell.setPadding(6);
            amtCell.setBorderColor(BORDER);
            amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(amtCell);

            grandTotal = grandTotal.add(entry.getValue());
            grandCount += count;
            alt = !alt;
        }

        // Grand total row
        PdfPCell gtLabel = new PdfPCell(new Phrase("TOTAL", f(10, Font.BOLD, BaseColor.WHITE)));
        gtLabel.setBackgroundColor(NAVY);
        gtLabel.setPadding(8);
        gtLabel.setBorderColor(NAVY2);
        if (filterCat == null) {
            t.addCell(gtLabel);
            PdfPCell gtCount = new PdfPCell(new Phrase(String.valueOf(grandCount), f(10, Font.BOLD, BaseColor.WHITE)));
            gtCount.setBackgroundColor(NAVY);
            gtCount.setPadding(8);
            gtCount.setBorderColor(NAVY2);
            t.addCell(gtCount);
        } else {
            t.addCell(gtLabel);
        }
        PdfPCell gtAmt = new PdfPCell(new Phrase("₹ " + fmt(grandTotal), f(10, Font.BOLD, BaseColor.WHITE)));
        gtAmt.setBackgroundColor(GOLD);
        gtAmt.setPadding(8);
        gtAmt.setBorderColor(GOLD);
        gtAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(gtAmt);

        doc.add(t);
    }

    // ── One category block with its own coloured header + detail rows ─────────
    private void addCategoryBlock(Document doc, ExpenseCategory category,
            List<Expense> expenses) throws DocumentException {

        BaseColor catColour = CAT_COLOURS.getOrDefault(category, MUTED);

        // Category heading bar
        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[] { 3f, 1f });
        bar.setSpacingBefore(4);
        bar.setSpacingAfter(0);

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBackgroundColor(catColour);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPadding(9);
        Paragraph catLabel = new Paragraph();
        catLabel.add(new Chunk("● ", f(12, Font.BOLD, BaseColor.WHITE)));
        catLabel.add(new Chunk(category.name() + "  (" + expenses.size() + " records)",
                f(10, Font.BOLD, BaseColor.WHITE)));
        nameCell.addElement(catLabel);
        bar.addCell(nameCell);

        BigDecimal catTotal = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PdfPCell totalCell = new PdfPCell(new Phrase("₹ " + fmt(catTotal), f(10, Font.BOLD, BaseColor.WHITE)));
        totalCell.setBackgroundColor(catColour);
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setPadding(9);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(totalCell);
        doc.add(bar);

        // Detail table
        PdfPTable detail = new PdfPTable(4);
        detail.setWidthPercentage(100);
        detail.setWidths(new float[] { 1.2f, 3.5f, 1.8f, 1.2f });
        detail.setSpacingAfter(2);

        String[] hdrs = { "Date", "Description", "Reference", "Amount ₹" };
        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, f(8, Font.BOLD, BaseColor.WHITE)));
            c.setBackgroundColor(NAVY2);
            c.setPadding(6);
            c.setBorderColor(NAVY);
            detail.addCell(c);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");
        boolean alt = false;
        for (Expense e : expenses) {
            BaseColor bg = alt ? LIGHT : BaseColor.WHITE;

            PdfPCell dateC = new PdfPCell(new Phrase(
                    e.getExpenseDate() != null ? e.getExpenseDate().format(df) : "-",
                    f(8, Font.NORMAL, DARK)));
            dateC.setBackgroundColor(bg);
            dateC.setPadding(5);
            dateC.setBorderColor(BORDER);
            detail.addCell(dateC);

            PdfPCell descC = new PdfPCell(new Phrase(e.getDescription(), f(8, Font.NORMAL, DARK)));
            descC.setBackgroundColor(bg);
            descC.setPadding(5);
            descC.setBorderColor(BORDER);
            detail.addCell(descC);

            PdfPCell refC = new PdfPCell(new Phrase(
                    e.getReference() != null ? e.getReference() : "-", f(8, Font.NORMAL, MUTED)));
            refC.setBackgroundColor(bg);
            refC.setPadding(5);
            refC.setBorderColor(BORDER);
            detail.addCell(refC);

            PdfPCell amtC = new PdfPCell(new Phrase("₹ " + fmt(e.getAmount()), f(8, Font.BOLD, DARK)));
            amtC.setBackgroundColor(bg);
            amtC.setPadding(5);
            amtC.setBorderColor(BORDER);
            amtC.setHorizontalAlignment(Element.ALIGN_RIGHT);
            detail.addCell(amtC);

            alt = !alt;
        }

        // Category subtotal footer row
        PdfPCell filler = new PdfPCell(new Phrase("Subtotal", f(9, Font.BOLD, DARK)));
        filler.setColspan(3);
        filler.setBackgroundColor(CAT_BG);
        filler.setPadding(6);
        filler.setBorderColor(BORDER);
        detail.addCell(filler);

        PdfPCell subT = new PdfPCell(new Phrase("₹ " + fmt(catTotal), f(9, Font.BOLD, DARK)));
        subT.setBackgroundColor(CAT_BG);
        subT.setPadding(6);
        subT.setBorderColor(BORDER);
        subT.setHorizontalAlignment(Element.ALIGN_RIGHT);
        detail.addCell(subT);

        doc.add(detail);
    }

    private String fmt(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ── Page footer ───────────────────────────────────────────────────────────
    static class PageFooter extends PdfPageEventHelper {

        private CompanyProperties co;

        PageFooter(CompanyProperties co) {
            this.co = co;
        }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            try {
                PdfContentByte cb = w.getDirectContent();
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.setColorStroke(new BaseColor(200, 151, 42, 90));
                cb.setLineWidth(0.5f);
                cb.moveTo(36, 44);
                cb.lineTo(559, 44);
                cb.stroke();
                cb.beginText();
                cb.setFontAndSize(bf, 7.5f);
                cb.setColorFill(new BaseColor(120, 140, 160));
                cb.showTextAligned(Element.ALIGN_LEFT,
                        co.getName() + " | " + co.getPhone(), 36, 30, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT,
                        "Page " + w.getPageNumber() + " | Confidential — Internal Use Only", 559, 30, 0);
                cb.endText();
            } catch (Exception ignore) {
            }
        }
    }
}
