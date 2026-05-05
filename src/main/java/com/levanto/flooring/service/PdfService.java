package com.levanto.flooring.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.levanto.flooring.config.CompanyProperties;
import com.levanto.flooring.entity.Transaction;
import com.levanto.flooring.entity.TransactionItem;
import com.levanto.flooring.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final CompanyProperties co;

    // Colours
    private static final BaseColor NAVY = new BaseColor(15, 35, 56);
    private static final BaseColor NAVY2 = new BaseColor(26, 58, 92);
    private static final BaseColor GOLD = new BaseColor(200, 151, 42);
    private static final BaseColor LIGHT = new BaseColor(243, 246, 250);
    private static final BaseColor BORDER = new BaseColor(210, 218, 228);
    private static final BaseColor DARK = new BaseColor(25, 25, 35);
    private static final BaseColor MUTED = new BaseColor(100, 118, 140);

    // Fonts (loaded lazily — iText built-ins only, no external files needed)
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

    public byte[] generate(Transaction tx) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(co));
            doc.open();

            header(doc, tx);
            infoRow(doc, tx);
            doc.add(Chunk.NEWLINE);
            itemsTable(doc, tx);
            totals(doc, tx);
            if (tx.getNotes() != null && !tx.getNotes().isBlank())
                notes(doc, tx.getNotes());
            terms(doc);

            if (tx.getStatus() == TransactionStatus.QUOTATION)
                watermark(writer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Header ──────────────────────────────────────────────────────────────
    private void header(Document doc, Transaction tx) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[] { 3.8f, 1.6f });
        t.setSpacingAfter(14);

        // Left: company
        PdfPCell left = cell(Rectangle.NO_BORDER);
        left.setBackgroundColor(NAVY);
        left.setPaddingLeft(18);
        left.setPaddingRight(18);
        left.setPaddingBottom(15);
        left.setPaddingTop(2);

        left.addElement(p(co.getName(), f(22, Font.BOLD, BaseColor.WHITE)));
        left.addElement(p(co.getTagline(), f(9, Font.NORMAL, BaseColor.WHITE)));
        left.addElement(p(co.getAddress(), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));
        left.addElement(p(co.getPhone() + "  |  " + co.getEmail(), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));
        left.addElement(p("GSTIN: " + co.getGstNumber(), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));

        t.addCell(left);

        // Right: doc badge
        boolean isBill = tx.getStatus() == TransactionStatus.FINAL_BILL;
        PdfPCell right = cell(Rectangle.NO_BORDER);
        right.setBackgroundColor(isBill ? GOLD : NAVY2);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setPaddingLeft(16);
        right.setPaddingRight(16);
        right.setPaddingBottom(16);
        right.setPaddingTop(2);
        Paragraph docType = p(isBill ? "TAX INVOICE" : "QUOTATION", f(14, Font.BOLD, BaseColor.WHITE));
        docType.setAlignment(Element.ALIGN_CENTER);
        right.addElement(docType);
        String num = isBill ? tx.getInvoiceNumber() : tx.getQuotationNumber();
        Paragraph numP = p(num != null ? num : "-", f(9, Font.NORMAL, BaseColor.WHITE));
        numP.setAlignment(Element.ALIGN_CENTER);
        right.addElement(numP);
        String dateStr = tx.getCreatedAt() != null
                ? tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : "";
        Paragraph dateP = p(dateStr, f(8, Font.NORMAL, BaseColor.WHITE));
        dateP.setAlignment(Element.ALIGN_CENTER);
        right.addElement(dateP);
        t.addCell(right);

        doc.add(t);
    }

    // ── Bill-To + Doc Details ────────────────────────────────────────────────
    private void infoRow(Document doc, Transaction tx) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[] { 1f, 1f });
        t.setSpacingAfter(8);

        // Bill to
        PdfPCell bto = cell(Rectangle.BOX);
        bto.setBorderColor(BORDER);
        bto.setBackgroundColor(LIGHT);
        bto.setPadding(12);
        bto.addElement(p("BILL TO", f(8, Font.BOLD, GOLD)));
        bto.addElement(p(tx.getCustomer().getName(), f(11, Font.BOLD, DARK)));
        bto.addElement(p("📞 " + tx.getCustomer().getPhone(), f(9, Font.NORMAL, DARK)));
        if (s(tx.getCustomer().getAddress()))
            bto.addElement(p(tx.getCustomer().getAddress(), f(8, Font.NORMAL, MUTED)));
        if (s(tx.getCustomer().getGstNumber()))
            bto.addElement(p("GSTIN: " + tx.getCustomer().getGstNumber(), f(8, Font.BOLD, DARK)));
        doc.add(t); // add before using bto further

        // Re-build fresh table for 2-col layout
        PdfPTable t2 = new PdfPTable(2);
        t2.setWidthPercentage(100);
        t2.setWidths(new float[] { 1f, 1f });
        t2.setSpacingAfter(8);
        t2.addCell(bto);

        PdfPCell det = cell(Rectangle.BOX);
        det.setBorderColor(BORDER);
        det.setBackgroundColor(LIGHT);
        det.setPadding(12);
        det.addElement(p("DOCUMENT DETAILS", f(8, Font.BOLD, GOLD)));
        det.addElement(detRow("Number", tx.getStatus() == TransactionStatus.FINAL_BILL
                ? tx.getInvoiceNumber()
                : tx.getQuotationNumber()));
        det.addElement(detRow("Date", tx.getCreatedAt() != null
                ? tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                : ""));
        det.addElement(detRow("Type", tx.getStatus() == TransactionStatus.FINAL_BILL ? "Tax Invoice" : "Quotation"));
        det.addElement(detRow("Payment", tx.getPaymentStatus().name().replace("_", " ")));
        if (tx.getConvertedAt() != null)
            det.addElement(detRow("Converted", tx.getConvertedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
        t2.addCell(det);
        doc.add(t2);
    }

    private Paragraph detRow(String label, String val) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", f(8, Font.BOLD, MUTED)));
        p.add(new Chunk(val != null ? val : "", f(8, Font.NORMAL, DARK)));
        p.setSpacingAfter(2);
        return p;
    }

    // ── Items table ──────────────────────────────────────────────────────────
    private void itemsTable(Document doc, Transaction tx) throws DocumentException {
        String[] hdrs = { "#", "Description", "HSN/SAC", "Qty", "Unit", "Rate", "GST%", "GST", "Total" };
        float[] wts = { 0.4f, 3.0f, 0.8f, 0.8f, 0.6f, 1.0f, 0.6f, 1.0f, 1.0f };

        PdfPTable t = new PdfPTable(hdrs.length);
        t.setWidthPercentage(100);
        t.setWidths(wts);
        t.setSpacingAfter(6);

        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, f(8, Font.BOLD, BaseColor.WHITE)));
            c.setBackgroundColor(NAVY);
            c.setPadding(7);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorderColor(NAVY2);
            t.addCell(c);
        }

        int n = 1;
        boolean alt = false;
        for (TransactionItem item : tx.getItems()) {
            BaseColor bg = alt ? LIGHT : BaseColor.WHITE;
            String desc = item.getDescription()
                    + (item.getItemType().name().equals("SERVICE") ? " [Svc]" : "");
            String sqft = item.getSquareFeet() != null && item.getSquareFeet().compareTo(BigDecimal.ZERO) > 0
                    ? fmt(item.getSquareFeet())
                    : fmt(item.getQuantity());
            String gstAmount = item.getIgstAmount() != null && item.getIgstAmount().compareTo(BigDecimal.ZERO) > 0
                    ? fmt(item.getIgstAmount())
                    : fmt(item.getCgstAmount().add(item.getSgstAmount()));

            row(t, bg, String.valueOf(n++), desc,
                    nv(item.getHsnSacCode()), sqft, nv(item.getUnit()),
                    fmt(item.getRatePerUnit()),
                    fmtPct(item.getGstPercent()),
                    gstAmount,
                    fmt(item.getTotalAmount()));
            alt = !alt;
        }
        doc.add(t);
    }

    private void row(PdfPTable t, BaseColor bg, String... vals) {
        boolean first = true;
        for (String v : vals) {
            PdfPCell c = new PdfPCell(new Phrase(v, f(8, Font.NORMAL, DARK)));
            c.setBackgroundColor(bg);
            c.setPadding(6);
            c.setBorderColor(BORDER);
            c.setHorizontalAlignment(first ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
            first = false;
            t.addCell(c);
        }
    }

    // ── Totals ───────────────────────────────────────────────────────────────
    private void totals(Document doc, Transaction tx) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(40);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setWidths(new float[] { 1.8f, 1f });
        t.setSpacingBefore(4);
        t.setSpacingAfter(12);

        totalRow(t, "Subtotal (excl. GST)", "₹ " + fmt(tx.getSubtotal()), false);

        BigDecimal mainGst = tx.getItems().stream()
                .filter(i -> i.getGstPercent() != null && i.getGstPercent().compareTo(BigDecimal.ZERO) > 0)
                .map(TransactionItem::getGstPercent)
                .findFirst().orElse(BigDecimal.ZERO);

        if (tx.isGstEnabled() || (tx.isTaxAllItems() && mainGst.compareTo(BigDecimal.ZERO) > 0)) {
            if (tx.getGstType() != null && tx.getGstType().name().equals("IGST")) {
                totalRow(t, "IGST (" + fmtPct(mainGst) + "%)", "₹ " + fmt(tx.getTotalIgst()), false);
            } else {
                BigDecimal halfGst = mainGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                totalRow(t, "CGST (" + fmtPct(halfGst) + "%)", "₹ " + fmt(tx.getTotalCgst()), false);
                totalRow(t, "SGST (" + fmtPct(halfGst) + "%)", "₹ " + fmt(tx.getTotalSgst()), false);
            }
        }

        // divider
        PdfPCell div = cell(Rectangle.TOP);
        div.setBorderColor(GOLD);
        div.setColspan(2);
        div.setFixedHeight(2);
        t.addCell(div);

        // Grand total
        PdfPCell gl = new PdfPCell(new Phrase("GRAND TOTAL", f(11, Font.BOLD, BaseColor.WHITE)));
        gl.setBackgroundColor(NAVY);
        gl.setPadding(10);
        gl.setBorder(Rectangle.NO_BORDER);
        t.addCell(gl);
        PdfPCell gv = new PdfPCell(new Phrase("₹ " + fmt(tx.getGrandTotal()), f(11, Font.BOLD, BaseColor.WHITE)));
        gv.setBackgroundColor(GOLD);
        gv.setPadding(10);
        gv.setBorder(Rectangle.NO_BORDER);
        gv.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(gv);

        // Amount paid
        totalRow(t, "Amount Paid", "₹ " + fmt(tx.getAmountPaid()), false);
        BigDecimal balance = tx.getGrandTotal().subtract(tx.getAmountPaid());
        totalRow(t, "Balance Due", "₹ " + fmt(balance), true);

        doc.add(t);

        Paragraph words = p("Amount in words: " + inWords(tx.getGrandTotal()) + " Only",
                f(8, Font.NORMAL, MUTED));
        words.setAlignment(Element.ALIGN_RIGHT);
        doc.add(words);
    }

    private void totalRow(PdfPTable t, String lbl, String val, boolean bold) {
        PdfPCell lc = new PdfPCell(new Phrase(lbl, f(9, bold ? Font.BOLD : Font.NORMAL, DARK)));
        lc.setPadding(5);
        lc.setBorderColor(BORDER);
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(val, f(9, Font.BOLD, DARK)));
        vc.setPadding(5);
        vc.setBorderColor(BORDER);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(vc);
    }

    private void notes(Document doc, String notes) throws DocumentException {
        doc.add(p("Notes:", f(9, Font.BOLD, GOLD)));
        doc.add(p(notes, f(9, Font.NORMAL, DARK)));
        doc.add(Chunk.NEWLINE);
    }

    private void terms(Document doc) throws DocumentException {
        doc.add(p("Terms & Conditions:", f(8, Font.BOLD, MUTED)));
        doc.add(p("• Goods once sold will not be taken back or exchanged.\n" +
                "• This is a computer-generated document, valid without signature.\n" +
                "• Subject to local jurisdiction only.", f(7, Font.NORMAL, MUTED)));
    }

    private void watermark(PdfWriter w) {
        try {
            PdfContentByte cb = w.getDirectContentUnder();
            cb.saveState();
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.05f);
            cb.setGState(gs);
            cb.beginText();
            cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false), 68);
            cb.setColorFill(GOLD);
            cb.showTextAligned(Element.ALIGN_CENTER, "QUOTATION", 298, 420, 45);
            cb.endText();
            cb.restoreState();
        } catch (Exception ignore) {
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private PdfPCell cell(int border) {
        PdfPCell c = new PdfPCell();
        c.setBorder(border);
        return c;
    }

    private Paragraph p(String txt, Font f) {
        return new Paragraph(txt, f);
    }

    private String nv(String s) {
        return s == null ? "" : s;
    }

    private boolean s(String v) {
        return v != null && !v.isBlank();
    }

    private String fmt(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String fmtPct(BigDecimal v) {
        if (v == null)
            return "0";
        if (v.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return String.valueOf(v.toBigInteger());
        }
        return v.stripTrailingZeros().toPlainString();
    }

    private String inWords(BigDecimal amount) {
        if (amount == null)
            return "Zero Rupees";
        long r = amount.longValue();
        long p = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).longValue();
        String res = words(r) + " Rupees";
        if (p > 0)
            res += " and " + words(p) + " Paise";
        return res;
    }

    private String words(long n) {
        if (n == 0)
            return "Zero";
        String[] o = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
                "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
        String[] t = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };
        if (n < 20)
            return o[(int) n];
        if (n < 100)
            return t[(int) (n / 10)] + (n % 10 != 0 ? " " + o[(int) (n % 10)] : "");
        if (n < 1000)
            return o[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + words(n % 100) : "");
        if (n < 100000)
            return words(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + words(n % 1000) : "");
        if (n < 10000000)
            return words(n / 100000) + " Lakh" + (n % 100000 != 0 ? " " + words(n % 100000) : "");
        return words(n / 10000000) + " Crore" + (n % 10000000 != 0 ? " " + words(n % 10000000) : "");
    }

    // ── Page footer ──────────────────────────────────────────────────────────
    static class PageFooter extends PdfPageEventHelper {
        private final CompanyProperties co;

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
                cb.showTextAligned(Element.ALIGN_LEFT, co.getName() + " | " + co.getPhone(), 36, 30, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Page " + w.getPageNumber() + " | Thank you for your business!",
                        559, 30, 0);
                cb.endText();
            } catch (Exception ignore) {
            }
        }
    }
}
