package com.levanto.flooring.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.levanto.flooring.config.CompanyProperties;
import com.levanto.flooring.entity.Attendance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendancePdfService {

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

    public byte[] generate(List<com.levanto.flooring.dto.AttendanceReportDto> attendances, Integer year, Integer month,
            String employeeName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(co));
            doc.open();

            // ── Document Header ───────────────────────────────────────────────
            addDocHeader(doc, year, month, employeeName);

            doc.add(Chunk.NEWLINE);

            // ── Detail table ──────────────────────────────────────────────────
            addDetailTable(doc, attendances);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Attendance PDF generation failed", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addDocHeader(Document doc, Integer year, Integer month, String employeeName) throws DocumentException {
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

        // Right: report badge
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(GOLD);
        right.setPaddingLeft(16);
        right.setPaddingRight(16);
        right.setPaddingBottom(16);
        right.setPaddingTop(2);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph title = new Paragraph("ATTENDANCE REPORT", f(11, Font.BOLD, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        right.addElement(title);

        Paragraph period = new Paragraph(month + " / " + year, f(10, Font.BOLD, NAVY));
        period.setAlignment(Element.ALIGN_CENTER);
        right.addElement(period);

        if (employeeName != null && !employeeName.isEmpty()) {
            Paragraph empP = new Paragraph("Employee: " + employeeName, f(9, Font.NORMAL, NAVY));
            empP.setAlignment(Element.ALIGN_CENTER);
            right.addElement(empP);
        }
        t.addCell(right);
        doc.add(t);
    }

    private void addDetailTable(Document doc, List<com.levanto.flooring.dto.AttendanceReportDto> attendances)
            throws DocumentException {
        PdfPTable detail = new PdfPTable(6);
        detail.setWidthPercentage(100);
        detail.setWidths(new float[] { 1.5f, 3f, 1.5f, 1.5f, 1.5f, 2f });
        detail.setSpacingAfter(10);

        String[] hdrs = { "Date", "Employee", "Clock In", "Clock Out", "Status", "Notes" };
        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, f(9, Font.BOLD, BaseColor.WHITE)));
            c.setBackgroundColor(NAVY2);
            c.setPadding(7);
            c.setBorderColor(NAVY);
            detail.addCell(c);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        boolean alt = false;

        int presentCount = 0;
        int absentCount = 0;
        int leaveCount = 0;
        int holidayCount = 0;

        for (com.levanto.flooring.dto.AttendanceReportDto a : attendances) {
            BaseColor bg = alt ? LIGHT : BaseColor.WHITE;

            if (a.getStatus() != null) {
                if ("PRESENT".equals(a.getStatus()) || "HALF_DAY".equals(a.getStatus()))
                    presentCount++;
                else if ("ABSENT".equals(a.getStatus()))
                    absentCount++;
                else if ("ON LEAVE".equals(a.getStatus()) || "LOSS OF PAY".equals(a.getStatus()))
                    leaveCount++;
                else if ("HOLIDAY".equals(a.getStatus()))
                    holidayCount++;
            }

            // Date
            PdfPCell dateC = new PdfPCell(
                    new Phrase(a.getDate() != null ? a.getDate().format(df) : "-", f(8, Font.NORMAL, DARK)));
            dateC.setBackgroundColor(bg);
            dateC.setPadding(6);
            dateC.setBorderColor(BORDER);
            detail.addCell(dateC);

            // Employee
            PdfPCell empC = new PdfPCell(new Phrase(a.getEmployee().getName(), f(8, Font.NORMAL, DARK)));
            empC.setBackgroundColor(bg);
            empC.setPadding(6);
            empC.setBorderColor(BORDER);
            detail.addCell(empC);

            // Clock In
            PdfPCell inC = new PdfPCell(new Phrase(a.getClockInTime() != null ? a.getClockInTime().format(tf) : "-",
                    f(8, Font.NORMAL, DARK)));
            inC.setBackgroundColor(bg);
            inC.setPadding(6);
            inC.setBorderColor(BORDER);
            detail.addCell(inC);

            // Clock Out
            PdfPCell outC = new PdfPCell(new Phrase(a.getClockOutTime() != null ? a.getClockOutTime().format(tf) : "-",
                    f(8, Font.NORMAL, DARK)));
            outC.setBackgroundColor(bg);
            outC.setPadding(6);
            outC.setBorderColor(BORDER);
            detail.addCell(outC);

            // Status
            String status = a.getStatus() != null ? a.getStatus() : "-";
            if (a.isManualCorrection())
                status += " (M)";
            BaseColor statusColor = DARK;
            if ("ABSENT".equals(a.getStatus()) || "LOSS OF PAY".equals(a.getStatus()))
                statusColor = new BaseColor(220, 38, 38);
            if ("PRESENT".equals(a.getStatus()))
                statusColor = new BaseColor(5, 150, 105);
            if ("HOLIDAY".equals(a.getStatus()) || "ON LEAVE".equals(a.getStatus()))
                statusColor = new BaseColor(37, 99, 235);
            if ("WEEKEND".equals(a.getStatus()))
                statusColor = new BaseColor(100, 116, 139);

            PdfPCell statC = new PdfPCell(new Phrase(status, f(8, Font.BOLD, statusColor)));
            statC.setBackgroundColor(bg);
            statC.setPadding(6);
            statC.setBorderColor(BORDER);
            detail.addCell(statC);

            // Notes
            PdfPCell noteC = new PdfPCell(
                    new Phrase(a.getNotes() != null ? a.getNotes() : "", f(8, Font.NORMAL, MUTED)));
            noteC.setBackgroundColor(bg);
            noteC.setPadding(6);
            noteC.setBorderColor(BORDER);
            detail.addCell(noteC);

            alt = !alt;
        }

        doc.add(detail);

        // Add Summary Info
        Paragraph summary = new Paragraph();
        summary.add(new Chunk("Summary: ", f(10, Font.BOLD, NAVY)));
        summary.add(new Chunk("Present: " + presentCount + "  |  ", f(9, Font.NORMAL, DARK)));
        summary.add(new Chunk("Absent: " + absentCount + "  |  ", f(9, Font.NORMAL, DARK)));
        summary.add(new Chunk("Leaves: " + leaveCount + "  |  ", f(9, Font.NORMAL, DARK)));
        summary.add(new Chunk("Holidays: " + holidayCount, f(9, Font.NORMAL, DARK)));
        summary.setSpacingBefore(10);
        doc.add(summary);
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
