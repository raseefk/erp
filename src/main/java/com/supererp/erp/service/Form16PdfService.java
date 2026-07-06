package com.supererp.erp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.supererp.erp.entity.CompanySettings;
import com.supererp.erp.entity.Employee;
import com.supererp.erp.entity.PayrollEntry;
import com.supererp.erp.repository.PayrollEntryRepository;
import com.supererp.erp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Generates a simplified Form 16 (Part B) — Annual salary certificate.
 * Covers: Gross annual salary, deductions (PF, ESI, PT, TDS),
 * and net taxable income for the financial year.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Form16PdfService {

    private final PayrollEntryRepository entryRepo;
    private final EmployeeRepository     empRepo;
    private final CompanySettingsService settingsService;

    private static final BaseColor NAVY  = new BaseColor(15, 35, 56);
    private static final BaseColor GOLD  = new BaseColor(200, 151, 42);
    private static final BaseColor LIGHT = new BaseColor(243, 246, 250);
    private static final BaseColor DARK  = new BaseColor(25, 25, 35);
    private static final BaseColor MUTED = new BaseColor(100, 118, 140);

    private Font bold(int size, BaseColor c) {
        try { return new Font(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false), size, Font.BOLD, c); }
        catch (Exception e) { return new Font(Font.FontFamily.HELVETICA, size, Font.BOLD, c); }
    }

    private Font normal(int size, BaseColor c) {
        try { return new Font(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), size, Font.NORMAL, c); }
        catch (Exception e) { return new Font(Font.FontFamily.HELVETICA, size, Font.NORMAL, c); }
    }

    @Transactional(readOnly = true)
    public byte[] generateForm16(Long employeeId, int financialYear) {
        Employee emp = empRepo.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        CompanySettings cs = settingsService.getSettings();

        // FY April–March: e.g. FY 2025-26 = months April 2025–March 2026
        // Collect payroll entries for the FY
        List<PayrollEntry> entries = entryRepo.findByEmployeeIdOrdered(employeeId).stream()
            .filter(e -> isInFinancialYear(e.getPayrollRun().getPayMonth(),
                                           e.getPayrollRun().getPayYear(), financialYear))
            .toList();

        // Aggregate annual figures
        BigDecimal annualGross   = sum(entries, PayrollEntry::getGrossSalary);
        BigDecimal annualBasic   = sum(entries, PayrollEntry::getBasic);
        BigDecimal annualHra     = sum(entries, PayrollEntry::getHra);
        BigDecimal annualDa      = sum(entries, PayrollEntry::getDa);
        BigDecimal annualSpl     = sum(entries, PayrollEntry::getSpecialAllowance);
        BigDecimal annualPfEmp   = sum(entries, PayrollEntry::getPfEmployee);
        BigDecimal annualEsiEmp  = sum(entries, PayrollEntry::getEsiEmployee);
        BigDecimal annualPt      = sum(entries, PayrollEntry::getProfessionalTax);
        BigDecimal annualTds     = sum(entries, PayrollEntry::getTds);
        BigDecimal annualNet     = sum(entries, PayrollEntry::getNetSalary);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(PageSize.A4, 40, 40, 40, 60);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent(cs, financialYear));
            doc.open();

            addForm16Header(doc, emp, cs, financialYear);
            addPartB(doc, annualBasic, annualHra, annualDa, annualSpl,
                     annualGross, annualPfEmp, annualEsiEmp, annualPt, annualTds, annualNet);
            addDeclaration(doc, cs);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Form 16 generation failed for employee {}", employeeId, e);
            throw new RuntimeException("Form 16 generation failed: " + e.getMessage(), e);
        }
    }

    private boolean isInFinancialYear(int month, int year, int fy) {
        // FY fy means April (fy) to March (fy+1)
        if (month >= 4) return year == fy;
        return year == fy + 1;
    }

    private BigDecimal sum(List<PayrollEntry> entries,
                            java.util.function.Function<PayrollEntry, BigDecimal> getter) {
        return entries.stream()
            .map(getter)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addForm16Header(com.itextpdf.text.Document doc, Employee emp, CompanySettings cs, int fy)
            throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100); t.setWidths(new float[]{3f, 1.8f}); t.setSpacingAfter(16);

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(NAVY); left.setBorder(Rectangle.NO_BORDER); left.setPadding(14);
        left.addElement(new Paragraph(cs.getCompanyName() != null ? cs.getCompanyName() : "Employer", bold(16, BaseColor.WHITE)));
        if (cs.getAddress() != null)
            left.addElement(new Paragraph(cs.getAddress(), normal(8, new BaseColor(170, 200, 230))));
        if (cs.getTaxNumber() != null)
            left.addElement(new Paragraph("TAN/GSTIN: " + cs.getTaxNumber(), normal(8, new BaseColor(170, 200, 230))));
        t.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(GOLD); right.setBorder(Rectangle.NO_BORDER); right.setPadding(14);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("FORM 16", bold(14, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        right.addElement(title);
        Paragraph fyP = new Paragraph("FY " + fy + "-" + (fy + 1), bold(10, NAVY));
        fyP.setAlignment(Element.ALIGN_CENTER);
        right.addElement(fyP);
        Paragraph subTitle = new Paragraph("Certificate of Salary\nPart B", normal(8, NAVY));
        subTitle.setAlignment(Element.ALIGN_CENTER);
        right.addElement(subTitle);
        t.addCell(right);
        doc.add(t);

        // Employee details
        PdfPTable emp2 = new PdfPTable(4);
        emp2.setWidthPercentage(100); emp2.setWidths(new float[]{1.2f, 2f, 1.2f, 2f}); emp2.setSpacingAfter(12);
        addLV(emp2, "Employee Name", emp.getName());
        addLV(emp2, "Employee Code", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "-");
        addLV(emp2, "Designation", emp.getDesignation() != null ? emp.getDesignation() : "-");
        addLV(emp2, "PAN", emp.getPanNumber() != null ? emp.getPanNumber() : "NOT FURNISHED");
        doc.add(emp2);
    }

    private void addPartB(com.itextpdf.text.Document doc,
                           BigDecimal basic, BigDecimal hra, BigDecimal da, BigDecimal spl,
                           BigDecimal gross, BigDecimal pf, BigDecimal esi, BigDecimal pt,
                           BigDecimal tds, BigDecimal net) throws DocumentException {

        Paragraph heading = new Paragraph("PART B — DETAILS OF SALARY PAID AND TAX DEDUCTED", bold(10, NAVY));
        heading.setSpacingAfter(6);
        doc.add(heading);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100); t.setWidths(new float[]{4f, 2f}); t.setSpacingAfter(14);

        // Earnings section header
        addSectionHeader(t, "I. GROSS SALARY", "₹");
        addRow(t, "    (a) Basic Salary", basic, true);
        addRow(t, "    (b) House Rent Allowance (HRA)", hra, false);
        addRow(t, "    (c) Dearness Allowance (DA)", da, true);
        addRow(t, "    (d) Special Allowance", spl, false);
        addTotalRow(t, "Gross Salary (a+b+c+d)", gross, true);

        // Deductions section
        addSectionHeader(t, "II. DEDUCTIONS FROM SALARY", "₹");
        addRow(t, "    (a) Provident Fund (Employee Contribution)", pf, true);
        addRow(t, "    (b) ESI (Employee Contribution)", esi, false);
        addRow(t, "    (c) Professional Tax", pt, true);
        BigDecimal totalDed = pf.add(esi).add(pt);
        addTotalRow(t, "Total Deductions (a+b+c)", totalDed, false);

        // Tax & net
        addSectionHeader(t, "III. TAX DEDUCTED AT SOURCE", "₹");
        addRow(t, "    TDS Deducted (Section 192 / 26QB)", tds, true);
        addTotalRow(t, "NET SALARY PAID", net, true);
        doc.add(t);
    }

    private void addSectionHeader(PdfPTable t, String label, String col2) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, bold(9, BaseColor.WHITE)));
        c1.setBackgroundColor(NAVY); c1.setPadding(6); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(col2, bold(9, BaseColor.WHITE)));
        c2.setBackgroundColor(NAVY); c2.setPadding(6); c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1); t.addCell(c2);
    }

    private void addRow(PdfPTable t, String label, BigDecimal val, boolean alt) {
        BaseColor bg = alt ? LIGHT : BaseColor.WHITE;
        PdfPCell c1 = new PdfPCell(new Phrase(label, normal(8, DARK)));
        c1.setBackgroundColor(bg); c1.setPadding(5); c1.setBorderColor(LIGHT);
        PdfPCell c2 = new PdfPCell(new Phrase(fmt(val), normal(8, DARK)));
        c2.setBackgroundColor(bg); c2.setPadding(5); c2.setBorderColor(LIGHT);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1); t.addCell(c2);
    }

    private void addTotalRow(PdfPTable t, String label, BigDecimal val, boolean gold) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, bold(9, gold ? NAVY : BaseColor.WHITE)));
        c1.setBackgroundColor(gold ? GOLD : NAVY); c1.setPadding(6); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(fmt(val), bold(9, gold ? NAVY : GOLD)));
        c2.setBackgroundColor(gold ? GOLD : NAVY); c2.setPadding(6); c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1); t.addCell(c2);
    }

    private void addDeclaration(com.itextpdf.text.Document doc, CompanySettings cs) throws DocumentException {
        Paragraph p = new Paragraph(
            "DECLARATION: I " + (cs.getCompanyName() != null ? cs.getCompanyName() : "the Employer") +
            " hereby certify that a sum of tax as mentioned above has been deducted from the salary of the " +
            "employee as per the provisions of Section 192 of the Income Tax Act, 1961. " +
            "This is a system-generated certificate.",
            normal(7, MUTED));
        p.setSpacingBefore(10);
        doc.add(p);
    }

    private void addLV(PdfPTable t, String l, String v) {
        PdfPCell lc = new PdfPCell(new Phrase(l, normal(7, MUTED)));
        lc.setBorder(Rectangle.BOTTOM); lc.setBorderColor(LIGHT); lc.setPadding(5); lc.setBackgroundColor(LIGHT);
        PdfPCell vc = new PdfPCell(new Phrase(v, bold(8, DARK)));
        vc.setBorder(Rectangle.BOTTOM); vc.setBorderColor(LIGHT); vc.setPadding(5);
        t.addCell(lc); t.addCell(vc);
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%,.2f", v.setScale(2, RoundingMode.HALF_UP));
    }

    static class FooterEvent extends PdfPageEventHelper {
        private final CompanySettings cs; private final int fy;
        FooterEvent(CompanySettings cs, int fy) { this.cs = cs; this.fy = fy; }
        @Override public void onEndPage(PdfWriter w, Document d) {
            try {
                PdfContentByte cb = w.getDirectContent();
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.setColorStroke(new BaseColor(200, 151, 42, 90)); cb.setLineWidth(0.5f);
                cb.moveTo(40, 44); cb.lineTo(555, 44); cb.stroke();
                cb.beginText(); cb.setFontAndSize(bf, 7f); cb.setColorFill(new BaseColor(120, 140, 160));
                cb.showTextAligned(Element.ALIGN_LEFT,
                    "Form 16 — FY " + fy + "-" + (fy+1) + " | " + (cs.getCompanyName() != null ? cs.getCompanyName() : ""), 40, 30, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Confidential | Page " + w.getPageNumber(), 555, 30, 0);
                cb.endText();
            } catch (Exception ignore) {}
        }
    }
}
