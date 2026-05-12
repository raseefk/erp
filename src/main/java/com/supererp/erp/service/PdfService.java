package com.supererp.erp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.supererp.erp.config.CompanyProperties;
import com.supererp.erp.entity.Project;
import com.supererp.erp.entity.Transaction;
import com.supererp.erp.entity.TransactionItem;
import com.supererp.erp.enums.TransactionStatus;
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

    private final com.supererp.erp.service.CompanySettingsService settingsService;

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
        return generate(tx, BigDecimal.ZERO);
    }

    public byte[] generate(Transaction tx, BigDecimal cashPaid) {
        try {
            com.supererp.erp.entity.CompanySettings settings = settingsService.getSettings();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(settings));
            doc.open();

            String title = tx.getStatus() == TransactionStatus.FINAL_BILL ? "TAX INVOICE" : "QUOTATION";
            String number = tx.getStatus() == TransactionStatus.FINAL_BILL ? tx.getInvoiceNumber() : tx.getQuotationNumber();
            String date = tx.getCreatedAt() != null ? tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
            header(doc, title, number, date, tx.getStatus() == TransactionStatus.FINAL_BILL, settings);
            infoRow(doc, tx);
            doc.add(Chunk.NEWLINE);
            itemsTable(doc, tx);
            totals(doc, tx, cashPaid);
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

    public byte[] generateAdvanceReceipt(com.supererp.erp.entity.AdvancePayment advance) {
        try {
            com.supererp.erp.entity.CompanySettings settings = settingsService.getSettings();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(settings));
            doc.open();

            // Header
            String dt = advance.getDate() != null ? advance.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
            header(doc, "ADVANCE RECEIPT", advance.getAdvanceNumber(), dt, true, settings);

            // Details
            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            t.setSpacingAfter(15);
            
            PdfPCell c = cell(Rectangle.BOX);
            c.setBorderColor(BORDER);
            c.setBackgroundColor(LIGHT);
            c.setPadding(15);
            
            c.addElement(p("RECEIVED WITH THANKS FROM:", f(8, Font.BOLD, GOLD)));
            c.addElement(p(advance.getPaymentFrom(), f(12, Font.BOLD, DARK)));
            
            if (advance.getProject() != null) {
                c.addElement(Chunk.NEWLINE);
                c.addElement(p("FOR PROJECT:", f(8, Font.BOLD, GOLD)));
                c.addElement(p(advance.getProject().getName(), f(10, Font.NORMAL, DARK)));
            }
            
            if (s(advance.getDescription())) {
                c.addElement(Chunk.NEWLINE);
                c.addElement(p("DESCRIPTION:", f(8, Font.BOLD, GOLD)));
                c.addElement(p(advance.getDescription(), f(10, Font.NORMAL, DARK)));
            }
            
            t.addCell(c);
            doc.add(t);

            // Amount Box
            PdfPTable aTable = new PdfPTable(1);
            aTable.setWidthPercentage(50);
            aTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            PdfPCell ac = cell(Rectangle.BOX);
            ac.setBackgroundColor(NAVY);
            ac.setPadding(12);
            Paragraph ap = p("Amount Received: ₹ " + fmt(advance.getAmount()), f(14, Font.BOLD, BaseColor.WHITE));
            ap.setAlignment(Element.ALIGN_RIGHT);
            ac.addElement(ap);
            aTable.addCell(ac);
            doc.add(aTable);

            doc.add(Chunk.NEWLINE);
            Paragraph words = p("Amount in words: " + inWords(advance.getAmount()) + " Only", f(9, Font.ITALIC, MUTED));
            words.setAlignment(Element.ALIGN_RIGHT);
            doc.add(words);

            // Signature
            Paragraph sig = p("Authorised Signatory", f(9, Font.BOLD, DARK));
            sig.setAlignment(Element.ALIGN_RIGHT);
            sig.setSpacingBefore(60);
            doc.add(sig);

            terms(doc);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Advance receipt PDF failed: {}", e.getMessage(), e);
            throw new RuntimeException("Advance receipt PDF failed", e);
        }
    }

    public byte[] generateProjectIncomeReport(Project project, java.util.List<java.util.Map<String, Object>> entries, BigDecimal totalReceived, BigDecimal totalPending) {
        try {
            com.supererp.erp.entity.CompanySettings settings = settingsService.getSettings();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(settings));
            doc.open();

            header(doc, "INCOME REPORT", project.getName(), java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), true, settings);

            // Project Info
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setSpacingAfter(15);
            PdfPCell left = cell(Rectangle.NO_BORDER);
            left.addElement(p("CLIENT: " + (project.getClientName() != null ? project.getClientName() : "-"), f(9, Font.BOLD, MUTED)));
            info.addCell(left);
            PdfPCell right = cell(Rectangle.NO_BORDER);
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pTotal = p("TOTAL RECEIVED: ₹ " + fmt(totalReceived), f(10, Font.BOLD, GOLD));
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(pTotal);
            info.addCell(right);
            doc.add(info);

            // Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 3.5f, 1.5f, 1.5f, 1.5f});
            table.setSpacingBefore(10);

            // Headers
            String[] headers = {"Date", "Description", "Grand Total", "Received", "Pending"};
            for (String h : headers) {
                PdfPCell hc = new PdfPCell(new Phrase(h, f(9, Font.BOLD, BaseColor.WHITE)));
                hc.setBackgroundColor(NAVY);
                hc.setPadding(6);
                hc.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(hc);
            }

            // Rows
            for (java.util.Map<String, Object> e : entries) {
                java.time.LocalDateTime dt = (java.time.LocalDateTime) e.get("date");
                table.addCell(cellTable(dt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), Element.ALIGN_CENTER));
                table.addCell(cellTable(e.get("title").toString(), Element.ALIGN_LEFT));
                table.addCell(cellTable(fmt((BigDecimal) e.get("total")), Element.ALIGN_RIGHT));
                table.addCell(cellTable(fmt((BigDecimal) e.get("received")), Element.ALIGN_RIGHT));
                table.addCell(cellTable(fmt((BigDecimal) e.get("pending")), Element.ALIGN_RIGHT));
            }

            // Totals Row
            PdfPCell tc = new PdfPCell(new Phrase("TOTALS", f(9, Font.BOLD, DARK)));
            tc.setColspan(3);
            tc.setBackgroundColor(LIGHT);
            tc.setPadding(6);
            tc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tc);
            
            PdfPCell tr = new PdfPCell(new Phrase(fmt(totalReceived), f(9, Font.BOLD, new BaseColor(22, 163, 74))));
            tr.setBackgroundColor(LIGHT);
            tr.setPadding(6);
            tr.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tr);

            PdfPCell tp = new PdfPCell(new Phrase(fmt(totalPending), f(9, Font.BOLD, new BaseColor(220, 38, 38))));
            tp.setBackgroundColor(LIGHT);
            tp.setPadding(6);
            tp.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tp);

            doc.add(table);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Income report PDF failed: {}", e.getMessage(), e);
            throw new RuntimeException("Income report PDF failed", e);
        }
    }

    public byte[] generateSubcontractorBill(com.supererp.erp.entity.SubcontractorRunningBill bill) {
        try {
            com.supererp.erp.entity.CompanySettings settings = settingsService.getSettings();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(settings));
            doc.open();

            // Header
            String dt = bill.getBillDate() != null ? bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
            header(doc, "RUNNING BILL", bill.getBillNumber(), dt, true, settings);

            // Details Table
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setSpacingAfter(15);
            info.setWidths(new float[]{1f, 1f});

            // Left: Subcontractor Info
            PdfPCell left = cell(Rectangle.BOX);
            left.setBorderColor(BORDER);
            left.setBackgroundColor(LIGHT);
            left.setPadding(12);
            left.addElement(p("SUBCONTRACTOR / VENDOR", f(8, Font.BOLD, GOLD)));
            left.addElement(p(bill.getVendor().getName(), f(11, Font.BOLD, DARK)));
            if (s(bill.getVendor().getPhone())) left.addElement(p("📞 " + bill.getVendor().getPhone(), f(9, Font.NORMAL, DARK)));
            if (s(bill.getVendor().getAddress())) left.addElement(p(bill.getVendor().getAddress(), f(8, Font.NORMAL, MUTED)));
            info.addCell(left);

            // Right: Bill Details
            PdfPCell right = cell(Rectangle.BOX);
            right.setBorderColor(BORDER);
            right.setBackgroundColor(LIGHT);
            right.setPadding(12);
            right.addElement(p("PROJECT DETAILS", f(8, Font.BOLD, GOLD)));
            right.addElement(p(bill.getProject().getName(), f(10, Font.BOLD, DARK)));
            right.addElement(detRow("Status", bill.getStatus().name()));
            if (bill.getPeriodFrom() != null && bill.getPeriodTo() != null) {
                right.addElement(detRow("Period", bill.getPeriodFrom().format(DateTimeFormatter.ofPattern("dd/MM/yy")) + " to " + bill.getPeriodTo().format(DateTimeFormatter.ofPattern("dd/MM/yy"))));
            }
            info.addCell(right);
            doc.add(info);

            // Items Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.0f, 0.8f, 1.2f, 1.2f, 1.2f, 1.2f});
            table.setSpacingBefore(10);

            // Headers
            String[] headers = {"Description", "Rate", "Claim Qty", "Claim Amt", "Cert Qty", "Cert Amt"};
            for (String h : headers) {
                PdfPCell hc = new PdfPCell(new Phrase(h, f(8, Font.BOLD, BaseColor.WHITE)));
                hc.setBackgroundColor(NAVY);
                hc.setPadding(6);
                hc.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(hc);
            }

            // Rows
            for (com.supererp.erp.entity.SubcontractorRunningBillItem item : bill.getItems()) {
                table.addCell(cellTable(item.getDescription(), Element.ALIGN_LEFT));
                table.addCell(cellTable(fmt(item.getRate()), Element.ALIGN_RIGHT));
                table.addCell(cellTable(fmt(item.getClaimedQuantity()), Element.ALIGN_CENTER));
                table.addCell(cellTable(fmt(item.getClaimedAmount()), Element.ALIGN_RIGHT));
                table.addCell(cellTable(fmt(item.getCertifiedQuantity()), Element.ALIGN_CENTER));
                table.addCell(cellTable(fmt(item.getCertifiedAmount()), Element.ALIGN_RIGHT));
            }

            // Totals Row
            PdfPCell tc = new PdfPCell(new Phrase("TOTAL BILL AMOUNT", f(9, Font.BOLD, DARK)));
            tc.setColspan(5);
            tc.setBackgroundColor(LIGHT);
            tc.setPadding(8);
            tc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tc);
            
            PdfPCell ta = new PdfPCell(new Phrase(fmt(bill.getCertifiedAmount()), f(10, Font.BOLD, GOLD)));
            ta.setBackgroundColor(LIGHT);
            ta.setPadding(8);
            ta.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(ta);

            doc.add(table);

            // Footer / Signatures
            PdfPTable sigs = new PdfPTable(2);
            sigs.setWidthPercentage(100);
            sigs.setSpacingBefore(40);
            
            PdfPCell s1 = cell(Rectangle.NO_BORDER);
            s1.addElement(p("__________________________", f(10, Font.NORMAL, DARK)));
            s1.addElement(p("Prepared By / Subcontractor", f(8, Font.BOLD, MUTED)));
            if (bill.getSubmittedBy() != null) s1.addElement(p(bill.getSubmittedBy().getFullName(), f(8, Font.NORMAL, DARK)));
            sigs.addCell(s1);

            PdfPCell s2 = cell(Rectangle.NO_BORDER);
            s2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph sp2 = p("__________________________", f(10, Font.NORMAL, DARK));
            sp2.setAlignment(Element.ALIGN_RIGHT);
            s2.addElement(sp2);
            Paragraph sp3 = p("Authorised Signatory", f(8, Font.BOLD, MUTED));
            sp3.setAlignment(Element.ALIGN_RIGHT);
            s2.addElement(sp3);
            if (bill.getCertifiedBy() != null) {
                Paragraph sp4 = p(bill.getCertifiedBy().getFullName(), f(8, Font.NORMAL, DARK));
                sp4.setAlignment(Element.ALIGN_RIGHT);
                s2.addElement(sp4);
            }
            sigs.addCell(s2);
            doc.add(sigs);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Subcontractor bill PDF failed: {}", e.getMessage(), e);
            throw new RuntimeException("Subcontractor bill PDF failed", e);
        }
    }

    public byte[] generateMilestonePdf(com.supererp.erp.entity.ProjectMilestone milestone) {
        try {
            com.supererp.erp.entity.CompanySettings settings = settingsService.getSettings();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(settings));
            doc.open();

            // Header
            String dt = milestone.getSubmittedAt() != null 
                ? milestone.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) 
                : java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            header(doc, "MILESTONE CERTIFICATE", "MS-" + milestone.getId(), dt, true, settings);

            // Project & Client Info
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setSpacingAfter(20);
            info.setWidths(new float[]{1.2f, 0.8f});

            PdfPCell left = cell(Rectangle.BOX);
            left.setBorderColor(BORDER);
            left.setBackgroundColor(LIGHT);
            left.setPadding(15);
            left.addElement(p("PROJECT DETAILS", f(8, Font.BOLD, GOLD)));
            left.addElement(p(milestone.getProject().getName(), f(12, Font.BOLD, NAVY)));
            if (s(milestone.getProject().getClientName())) {
                left.addElement(Chunk.NEWLINE);
                left.addElement(p("CLIENT", f(8, Font.BOLD, GOLD)));
                left.addElement(p(milestone.getProject().getClientName(), f(10, Font.NORMAL, DARK)));
            }
            info.addCell(left);

            PdfPCell right = cell(Rectangle.BOX);
            right.setBorderColor(BORDER);
            right.setBackgroundColor(LIGHT);
            right.setPadding(15);
            right.addElement(p("MILESTONE STATUS", f(8, Font.BOLD, GOLD)));
            Paragraph statusP = p(milestone.getStatus().name().replace("_", " "), f(10, Font.BOLD, GOLD));
            right.addElement(statusP);
            right.addElement(Chunk.NEWLINE);
            right.addElement(p("DUE DATE", f(8, Font.BOLD, GOLD)));
            right.addElement(p(milestone.getDueDate() != null ? milestone.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "-", f(10, Font.NORMAL, DARK)));
            info.addCell(right);
            doc.add(info);

            // Milestone Content
            doc.add(p("MILESTONE DESCRIPTION", f(10, Font.BOLD, NAVY)));
            doc.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(0.5f, 100, BORDER, Element.ALIGN_CENTER, -2)));
            doc.add(Chunk.NEWLINE);
            
            Paragraph title = p(milestone.getName(), f(14, Font.BOLD, DARK));
            title.setSpacingAfter(10);
            doc.add(title);

            if (s(milestone.getDescription())) {
                Paragraph desc = p(milestone.getDescription(), f(10, Font.NORMAL, DARK));
                desc.setLeading(14);
                desc.setSpacingAfter(20);
                doc.add(desc);
            }

            // Financial Summary
            PdfPTable fin = new PdfPTable(2);
            fin.setWidthPercentage(100);
            fin.setSpacingBefore(20);
            fin.setWidths(new float[]{1f, 1f});

            PdfPCell f1 = cell(Rectangle.BOX);
            f1.setPadding(15);
            f1.setBorderColor(BORDER);
            f1.addElement(p("RELEASE PERCENTAGE", f(8, Font.BOLD, MUTED)));
            f1.addElement(p(fmtPct(milestone.getReleasePercent()) + "%", f(16, Font.BOLD, DARK)));
            fin.addCell(f1);

            PdfPCell f2 = cell(Rectangle.BOX);
            f2.setPadding(15);
            f2.setBorderColor(BORDER);
            f2.setBackgroundColor(NAVY);
            f2.addElement(p("RELEASE AMOUNT", f(8, Font.BOLD, GOLD)));
            f2.addElement(p("₹ " + fmt(milestone.getReleaseAmount()), f(16, Font.BOLD, BaseColor.WHITE)));
            fin.addCell(f2);
            doc.add(fin);

            // Client Approval Box
            if (milestone.getClientApprovedAt() != null) {
                doc.add(Chunk.NEWLINE);
                PdfPTable app = new PdfPTable(1);
                app.setWidthPercentage(100);
                PdfPCell ac = cell(Rectangle.BOX);
                ac.setPadding(12);
                ac.setBorderColor(new BaseColor(22, 163, 74));
                ac.setBackgroundColor(new BaseColor(240, 253, 244));
                ac.addElement(p("✓ CLIENT APPROVED", f(9, Font.BOLD, new BaseColor(22, 163, 74))));
                ac.addElement(p("Approved on: " + milestone.getClientApprovedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), f(8, Font.NORMAL, DARK)));
                if (s(milestone.getClientApprovalReference())) {
                    ac.addElement(p("Reference: " + milestone.getClientApprovalReference(), f(8, Font.ITALIC, MUTED)));
                }
                app.addCell(ac);
                doc.add(app);
            }

            // Signatures
            PdfPTable sigs = new PdfPTable(2);
            sigs.setWidthPercentage(100);
            sigs.setSpacingBefore(60);
            
            PdfPCell s1 = cell(Rectangle.NO_BORDER);
            s1.addElement(p("__________________________", f(10, Font.NORMAL, DARK)));
            s1.addElement(p("Project Manager", f(8, Font.BOLD, MUTED)));
            sigs.addCell(s1);

            PdfPCell s2 = cell(Rectangle.NO_BORDER);
            s2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph sp2 = p("__________________________", f(10, Font.NORMAL, DARK));
            sp2.setAlignment(Element.ALIGN_RIGHT);
            s2.addElement(sp2);
            Paragraph sp3 = p("Client Representative", f(8, Font.BOLD, MUTED));
            sp3.setAlignment(Element.ALIGN_RIGHT);
            s2.addElement(sp3);
            sigs.addCell(s2);
            doc.add(sigs);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Milestone PDF failed: {}", e.getMessage(), e);
            throw new RuntimeException("Milestone PDF failed", e);
        }
    }

    private PdfPCell cellTable(String txt, int align) {

        PdfPCell c = new PdfPCell(new Phrase(txt, f(8, Font.NORMAL, DARK)));
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        c.setBorderColor(BORDER);
        return c;
    }

    // ── Header ──────────────────────────────────────────────────────────────
    private void header(Document doc, String title, String number, String date, boolean isGold, com.supererp.erp.entity.CompanySettings settings) throws DocumentException {
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

        left.addElement(p(settings.getCompanyName(), f(22, Font.BOLD, BaseColor.WHITE)));
        if (settings.getTagline() != null && !settings.getTagline().isBlank()) {
            left.addElement(p(settings.getTagline(), f(9, Font.NORMAL, BaseColor.WHITE)));
        }
        
        left.addElement(p(settings.getAddress() != null ? settings.getAddress() : "", f(8, Font.NORMAL, new BaseColor(170, 195, 225))));
        left.addElement(p((settings.getPhone() != null ? settings.getPhone() : "") + "  |  " + (settings.getEmail() != null ? settings.getEmail() : ""), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));
        left.addElement(p("GSTIN: " + (settings.getTaxNumber() != null ? settings.getTaxNumber() : ""), f(8, Font.NORMAL, new BaseColor(170, 195, 225))));

        t.addCell(left);

        // Right: doc badge
        PdfPCell right = cell(Rectangle.NO_BORDER);
        right.setBackgroundColor(isGold ? GOLD : NAVY2);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setPaddingLeft(16);
        right.setPaddingRight(16);
        right.setPaddingBottom(16);
        right.setPaddingTop(2);
        Paragraph docType = p(title, f(14, Font.BOLD, BaseColor.WHITE));
        docType.setAlignment(Element.ALIGN_CENTER);
        right.addElement(docType);
        Paragraph numP = p(number != null ? number : "-", f(9, Font.NORMAL, BaseColor.WHITE));
        numP.setAlignment(Element.ALIGN_CENTER);
        right.addElement(numP);
        Paragraph dateP = p(date != null ? date : "", f(8, Font.NORMAL, BaseColor.WHITE));
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
    private void totals(Document doc, Transaction tx, BigDecimal cashPaid) throws DocumentException {
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

        // Advance + cash paid rows (cashPaid = sum from income transactions, not tx.amountPaid)
        BigDecimal advAmount = tx.getAdvanceSettledAmount() != null ? tx.getAdvanceSettledAmount() : BigDecimal.ZERO;
        if (advAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalRow(t, "Advance Adjusted", "-₹ " + fmt(advAmount), false);
        }
        if (cashPaid.compareTo(BigDecimal.ZERO) > 0) {
            totalRow(t, "Amount Paid", "₹ " + fmt(cashPaid), false);
        }
        BigDecimal balance = tx.getGrandTotal().subtract(advAmount).subtract(cashPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
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
        private final com.supererp.erp.entity.CompanySettings settings;

        PageFooter(com.supererp.erp.entity.CompanySettings settings) {
            this.settings = settings;
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
                cb.showTextAligned(Element.ALIGN_LEFT, settings.getCompanyName() + " | " + (settings.getPhone() != null ? settings.getPhone() : ""), 36, 30, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Page " + w.getPageNumber() + " | Thank you for your business!",
                        559, 30, 0);
                cb.endText();
            } catch (Exception ignore) {
            }
        }
    }
}
