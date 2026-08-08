package com.supererp.erp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.supererp.erp.entity.CompanySettings;
import com.supererp.erp.entity.Employee;
import com.supererp.erp.entity.PayrollEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Generates a single-page payslip PDF using iText 5.
 * Matches the existing navy/gold brand used throughout the app.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipPdfService {

    private final CompanySettingsService settingsService;

    // ── Brand colours ─────────────────────────────────────────────────────────
    private static final BaseColor NAVY  = new BaseColor(15, 35, 56);
    private static final BaseColor GOLD  = new BaseColor(200, 151, 42);
    private static final BaseColor LIGHT = new BaseColor(243, 246, 250);
    private static final BaseColor MUTED = new BaseColor(100, 118, 140);
    private static final BaseColor DARK  = new BaseColor(25, 25, 35);
    private static final BaseColor GREEN = new BaseColor(22, 163, 74);
    private static final BaseColor RED   = new BaseColor(220, 38, 38);

    private Font bold(int size, BaseColor c) {
        try {
            return new Font(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false), size, Font.BOLD, c);
        } catch (Exception e) { return new Font(Font.FontFamily.HELVETICA, size, Font.BOLD, c); }
    }

    private Font normal(int size, BaseColor c) {
        try {
            return new Font(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), size, Font.NORMAL, c);
        } catch (Exception e) { return new Font(Font.FontFamily.HELVETICA, size, Font.NORMAL, c); }
    }

    public byte[] generatePayslip(PayrollEntry entry) {
        try {
            CompanySettings cs = settingsService.getSettings();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent(cs));
            doc.open();

            addHeader(doc, entry, cs);
            addEmployeeInfo(doc, entry);
            addEarningsDeductions(doc, entry);
            addNetSalaryBox(doc, entry);
            addBankInfo(doc, entry);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Payslip PDF generation failed for entry {}", entry.getId(), e);
            throw new RuntimeException("Payslip PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private void addHeader(com.itextpdf.text.Document doc, PayrollEntry entry, CompanySettings cs) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{3f, 1.8f});
        t.setSpacingAfter(12);

        // Left: company info
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setBackgroundColor(NAVY);
        left.setPadding(14);
        left.addElement(new Paragraph(cs.getCompanyName() != null ? cs.getCompanyName() : "Company", bold(18, BaseColor.WHITE)));
        if (cs.getTagline() != null && !cs.getTagline().isBlank())
            left.addElement(new Paragraph(cs.getTagline(), normal(8, new BaseColor(180, 210, 240))));
        if (cs.getAddress() != null)
            left.addElement(new Paragraph(cs.getAddress(), normal(7, new BaseColor(160, 190, 220))));
        if (cs.getTaxNumber() != null)
            left.addElement(new Paragraph("GSTIN: " + cs.getTaxNumber(), normal(7, new BaseColor(160, 190, 220))));
        t.addCell(left);

        // Right: payslip badge
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(GOLD);
        right.setPadding(14);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("PAYSLIP", bold(13, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        right.addElement(title);
        Paragraph period = new Paragraph(entry.getPayrollRun().getPayPeriodLabel(), bold(11, NAVY));
        period.setAlignment(Element.ALIGN_CENTER);
        right.addElement(period);
        Paragraph status = new Paragraph(entry.getPayrollRun().getStatus().name(), normal(8, NAVY));
        status.setAlignment(Element.ALIGN_CENTER);
        right.addElement(status);
        t.addCell(right);
        doc.add(t);
    }

    // ── Employee info row ─────────────────────────────────────────────────────
    private void addEmployeeInfo(com.itextpdf.text.Document doc, PayrollEntry entry) throws DocumentException {
        Employee emp = entry.getEmployee();
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
        t.setSpacingAfter(10);
        t.setSpacingBefore(4);

        addInfoCell(t, "Employee Name", emp.getName());
        addInfoCell(t, "Employee Code", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "-");
        addInfoCell(t, "Designation", emp.getDesignation() != null ? emp.getDesignation() : "-");
        addInfoCell(t, "Department", emp.getDepartment() != null ? emp.getDepartment() : "-");
        addInfoCell(t, "PAN Number", emp.getPanNumber() != null ? emp.getPanNumber() : "-");
        addInfoCell(t, "PF Number", emp.getPfNumber() != null ? emp.getPfNumber() : "-");
        addInfoCell(t, "ESI Number", emp.getEsiNumber() != null ? emp.getEsiNumber() : "-");
        addInfoCell(t, "Days Present", String.valueOf(entry.getDaysPresent() != null ? entry.getDaysPresent() : 0));
        addInfoCell(t, "Working Days", String.valueOf(entry.getTotalWorkingDays() != null ? entry.getTotalWorkingDays() : 0));
        addInfoCell(t, "LOP Days", String.valueOf(entry.getDaysLop() != null ? entry.getDaysLop() : 0));
        doc.add(t);
    }

    private void addInfoCell(PdfPTable t, String label, String value) {
        PdfPCell lc = new PdfPCell(new Phrase(label, normal(7, MUTED)));
        lc.setBorder(Rectangle.BOTTOM);
        lc.setBorderColor(LIGHT);
        lc.setPadding(5);
        lc.setBackgroundColor(LIGHT);
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, bold(8, DARK)));
        vc.setBorder(Rectangle.BOTTOM);
        vc.setBorderColor(LIGHT);
        vc.setPadding(5);
        t.addCell(vc);
    }

    // ── Earnings / Deductions table ───────────────────────────────────────────
    private void addEarningsDeductions(com.itextpdf.text.Document doc, PayrollEntry entry) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{2.5f, 1.5f, 2.5f, 1.5f});
        t.setSpacingAfter(10);

        // Header row
        addColHeader(t, "EARNINGS", true);
        addColHeader(t, "AMOUNT (₹)", true);
        addColHeader(t, "DEDUCTIONS", false);
        addColHeader(t, "AMOUNT (₹)", false);

        // Rows
        addEDRow(t, "Basic Salary", entry.getBasic(), "PF (Employee)", entry.getPfEmployee(), true);
        addEDRow(t, "HRA", entry.getHra(), "ESI (Employee)", entry.getEsiEmployee(), false);
        addEDRow(t, "Dearness Allowance", entry.getDa(), "Professional Tax", entry.getProfessionalTax(), true);
        addEDRow(t, "Special Allowance", entry.getSpecialAllowance(), "TDS (26QB)", entry.getTds(), false);
        if (entry.getArrears().compareTo(BigDecimal.ZERO) > 0) {
            addEDRow(t, "Arrears", entry.getArrears(), "", BigDecimal.ZERO, true);
        }
        if (entry.getLeaveEncashment().compareTo(BigDecimal.ZERO) > 0) {
            addEDRow(t, "Leave Encashment", entry.getLeaveEncashment(), "", BigDecimal.ZERO, false);
        }

        // Totals row
        PdfPCell gtLabel = new PdfPCell(new Phrase("Gross Salary", bold(9, BaseColor.WHITE)));
        gtLabel.setBackgroundColor(NAVY); gtLabel.setPadding(6); gtLabel.setBorder(Rectangle.NO_BORDER);
        PdfPCell gtVal = new PdfPCell(new Phrase(fmt(entry.getGrossSalary()), bold(9, GOLD)));
        gtVal.setBackgroundColor(NAVY); gtVal.setPadding(6); gtVal.setBorder(Rectangle.NO_BORDER);
        gtVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell tdLabel = new PdfPCell(new Phrase("Total Deductions", bold(9, BaseColor.WHITE)));
        tdLabel.setBackgroundColor(NAVY); tdLabel.setPadding(6); tdLabel.setBorder(Rectangle.NO_BORDER);
        PdfPCell tdVal = new PdfPCell(new Phrase(fmt(entry.getTotalDeductions()), bold(9, RED)));
        tdVal.setBackgroundColor(NAVY); tdVal.setPadding(6); tdVal.setBorder(Rectangle.NO_BORDER);
        tdVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(gtLabel); t.addCell(gtVal); t.addCell(tdLabel); t.addCell(tdVal);

        doc.add(t);
    }

    private void addColHeader(PdfPTable t, String text, boolean isEarnings) {
        PdfPCell c = new PdfPCell(new Phrase(text, bold(8, BaseColor.WHITE)));
        c.setBackgroundColor(isEarnings ? NAVY : new BaseColor(60, 40, 10));
        c.setPadding(6); c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void addEDRow(PdfPTable t, String eLbl, BigDecimal eAmt,
                           String dLbl, BigDecimal dAmt, boolean alt) {
        BaseColor bg = alt ? LIGHT : BaseColor.WHITE;
        PdfPCell c1 = cell(eLbl, normal(8, DARK), bg);
        PdfPCell c2 = amtCell(eAmt, alt, GREEN);
        PdfPCell c3 = cell(dLbl, normal(8, DARK), bg);
        PdfPCell c4 = amtCell(dAmt, alt, dAmt != null && dAmt.compareTo(BigDecimal.ZERO) > 0 ? RED : MUTED);
        t.addCell(c1); t.addCell(c2); t.addCell(c3); t.addCell(c4);
    }

    private PdfPCell cell(String text, Font font, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg); c.setPadding(5); c.setBorderColor(LIGHT);
        return c;
    }

    private PdfPCell amtCell(BigDecimal amt, boolean alt, BaseColor valColor) {
        String display = (amt == null || amt.compareTo(BigDecimal.ZERO) == 0) ? "-" : fmt(amt);
        PdfPCell c = new PdfPCell(new Phrase(display, bold(8, valColor)));
        c.setBackgroundColor(alt ? LIGHT : BaseColor.WHITE);
        c.setPadding(5); c.setBorderColor(LIGHT);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    // ── Net Salary box ────────────────────────────────────────────────────────
    private void addNetSalaryBox(com.itextpdf.text.Document doc, PayrollEntry entry) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(GOLD); c.setPadding(10); c.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph("NET SALARY PAYABLE   ₹ " +
            entry.getNetSalary().setScale(2, RoundingMode.HALF_UP).toPlainString(), bold(14, NAVY));
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);
        if (entry.getPfEmployer().compareTo(BigDecimal.ZERO) > 0
                || entry.getEsiEmployer().compareTo(BigDecimal.ZERO) > 0) {
            Paragraph note = new Paragraph(
                "Employer PF: ₹" + fmt(entry.getPfEmployer()) +
                "   |   Employer ESI: ₹" + fmt(entry.getEsiEmployer()), normal(8, NAVY));
            note.setAlignment(Element.ALIGN_CENTER);
            c.addElement(note);
        }
        t.addCell(c);
        doc.add(t);
    }

    // ── Bank info ─────────────────────────────────────────────────────────────
    private void addBankInfo(com.itextpdf.text.Document doc, PayrollEntry entry) throws DocumentException {
        if (entry.getAccountNumber() == null || entry.getAccountNumber().isBlank()) return;
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(70);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setSpacingAfter(14);

        addInfoCell(t, "Bank Name", entry.getBankName() != null ? entry.getBankName() : "-");
        addInfoCell(t, "Account Number", entry.getAccountNumber());
        addInfoCell(t, "IFSC Code", entry.getIfscCode() != null ? entry.getIfscCode() : "-");
        doc.add(t);
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%,.2f", v);
    }

    // ── Page footer ───────────────────────────────────────────────────────────
    static class FooterEvent extends PdfPageEventHelper {
        private final CompanySettings cs;
        FooterEvent(CompanySettings cs) { this.cs = cs; }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            try {
                PdfContentByte cb = w.getDirectContent();
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.setColorStroke(new BaseColor(200, 151, 42, 90));
                cb.setLineWidth(0.5f);
                cb.moveTo(36, 44); cb.lineTo(559, 44); cb.stroke();
                cb.beginText();
                cb.setFontAndSize(bf, 7.5f);
                cb.setColorFill(new BaseColor(120, 140, 160));
                cb.showTextAligned(Element.ALIGN_LEFT,
                    (cs.getCompanyName() != null ? cs.getCompanyName() : "") +
                    " | This is a system-generated payslip.", 36, 30, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT,
                    "Confidential — Page " + w.getPageNumber(), 559, 30, 0);
                cb.endText();
            } catch (Exception ignore) {}
        }
    }
}
