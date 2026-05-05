package com.levanto.flooring.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.levanto.flooring.config.CompanyProperties;
import com.levanto.flooring.entity.DailyLabourLog;
import com.levanto.flooring.entity.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabourWagePdfService {

    private final CompanyProperties co;

    private static final BaseColor NAVY = new BaseColor(15, 35, 56);
    private static final BaseColor NAVY2 = new BaseColor(26, 58, 92);
    private static final BaseColor GOLD = new BaseColor(200, 151, 42);
    private static final BaseColor LIGHT = new BaseColor(243, 246, 250);
    private static final BaseColor BORDER = new BaseColor(210, 218, 228);
    private static final BaseColor DARK = new BaseColor(25, 25, 35);
    private static final BaseColor MUTED = new BaseColor(100, 118, 140);
    private static final BaseColor CAT_BG = new BaseColor(232, 239, 248);

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

    public byte[] generate(List<DailyLabourLog> logs, Project project, LocalDate from, LocalDate to) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(co));
            doc.open();

            addDocHeader(doc, project, from, to);

            Map<String, List<DailyLabourLog>> grouped = logs.stream()
                    .collect(Collectors.groupingBy(l -> l.getProjectLabour().getName(),
                            LinkedHashMap::new, Collectors.toList()));

            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Map.Entry<String, List<DailyLabourLog>> entry : grouped.entrySet()) {
                BigDecimal subtotal = addLabourBlock(doc, entry.getKey(), entry.getValue());
                grandTotal = grandTotal.add(subtotal);
                doc.add(new Paragraph(" "));
            }

            addGrandTotal(doc, grandTotal);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Labour Wage PDF generation failed", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addDocHeader(Document doc, Project project, LocalDate from, LocalDate to) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[] { 3f, 1.6f });
        t.setSpacingAfter(14);

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

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(GOLD);
        right.setPaddingLeft(16);
        right.setPaddingRight(16);
        right.setPaddingBottom(16);
        right.setPaddingTop(2);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph title = new Paragraph("LABOUR WAGE REPORT", f(11, Font.BOLD, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        right.addElement(title);

        Paragraph proj = new Paragraph("Project: " + project.getName(), f(9, Font.BOLD, NAVY));
        proj.setAlignment(Element.ALIGN_CENTER);
        right.addElement(proj);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String periodText = (from != null ? from.format(fmt) : "Start") + " – "
                + (to != null ? to.format(fmt) : "Present");
        Paragraph period = new Paragraph(periodText, f(8, Font.NORMAL, NAVY));
        period.setAlignment(Element.ALIGN_CENTER);
        right.addElement(period);

        t.addCell(right);
        doc.add(t);
    }

    private BigDecimal addLabourBlock(Document doc, String labourName, List<DailyLabourLog> logs)
            throws DocumentException {
        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[] { 3f, 1f });
        bar.setSpacingBefore(4);
        bar.setSpacingAfter(0);

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBackgroundColor(NAVY2);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPadding(9);
        Paragraph catLabel = new Paragraph();
        catLabel.add(new Chunk("👤 ", f(12, Font.BOLD, BaseColor.WHITE)));
        catLabel.add(new Chunk(labourName + "  (" + logs.size() + " days)", f(10, Font.BOLD, BaseColor.WHITE)));
        nameCell.addElement(catLabel);
        bar.addCell(nameCell);

        BigDecimal catTotal = logs.stream()
                .map(DailyLabourLog::getWagePaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPCell totalCell = new PdfPCell(new Phrase("₹ " + fmt(catTotal), f(10, Font.BOLD, BaseColor.WHITE)));
        totalCell.setBackgroundColor(NAVY2);
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setPadding(9);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(totalCell);
        doc.add(bar);

        PdfPTable detail = new PdfPTable(2);
        detail.setWidthPercentage(100);
        detail.setWidths(new float[] { 1f, 1f });
        detail.setSpacingAfter(2);

        String[] hdrs = { "Date", "Wage ₹" };
        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, f(8, Font.BOLD, BaseColor.WHITE)));
            c.setBackgroundColor(NAVY);
            c.setPadding(6);
            c.setBorderColor(NAVY2);
            detail.addCell(c);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");
        boolean alt = false;
        for (DailyLabourLog l : logs) {
            BaseColor bg = alt ? LIGHT : BaseColor.WHITE;

            PdfPCell dateC = new PdfPCell(new Phrase(
                    l.getDailyLog().getLogDate() != null ? l.getDailyLog().getLogDate().format(df) : "-",
                    f(8, Font.NORMAL, DARK)));
            dateC.setBackgroundColor(bg);
            dateC.setPadding(5);
            dateC.setBorderColor(BORDER);
            detail.addCell(dateC);

            PdfPCell amtC = new PdfPCell(new Phrase("₹ " + fmt(l.getWagePaid()), f(8, Font.BOLD, DARK)));
            amtC.setBackgroundColor(bg);
            amtC.setPadding(5);
            amtC.setBorderColor(BORDER);
            amtC.setHorizontalAlignment(Element.ALIGN_RIGHT);
            detail.addCell(amtC);

            alt = !alt;
        }

        PdfPCell filler = new PdfPCell(new Phrase("Subtotal", f(9, Font.BOLD, DARK)));
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
        return catTotal;
    }

    private void addGrandTotal(Document doc, BigDecimal grandTotal) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(50);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setWidths(new float[] { 1f, 1f });
        t.setSpacingBefore(10);

        PdfPCell gtLabel = new PdfPCell(new Phrase("PROJECT TOTAL", f(10, Font.BOLD, BaseColor.WHITE)));
        gtLabel.setBackgroundColor(NAVY);
        gtLabel.setPadding(8);
        gtLabel.setBorderColor(NAVY2);
        t.addCell(gtLabel);

        PdfPCell gtAmt = new PdfPCell(new Phrase("₹ " + fmt(grandTotal), f(10, Font.BOLD, BaseColor.WHITE)));
        gtAmt.setBackgroundColor(GOLD);
        gtAmt.setPadding(8);
        gtAmt.setBorderColor(GOLD);
        gtAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(gtAmt);

        doc.add(t);
    }

    private String fmt(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

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
