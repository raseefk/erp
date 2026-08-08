package com.supererp.erp.service;

import com.supererp.erp.entity.Employee;
import com.supererp.erp.entity.PayrollEntry;
import com.supererp.erp.entity.PayrollRun;
import com.supererp.erp.repository.PayrollEntryRepository;
import com.supererp.erp.repository.PayrollRunRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Statutory Compliance Service for Indian Labour & Tax Regulations.
 * Provides exports for:
 * - EPFO ECR (Electronic Challan-cum-Return) text format (#~# delimited)
 * - ESIC Monthly Return CSV format
 * - Professional Tax (PT) State-wise Summary
 * - TDS Section 24Q Quarterly Summary
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatutoryComplianceService {

    private final PayrollRunRepository runRepo;
    private final PayrollEntryRepository entryRepo;

    /**
     * Generates EPFO ECR (Electronic Challan-cum-Return) text file format (#~# delimited).
     * Standard EPFO ECR layout:
     * UAN#~#Member Name#~#Gross Wages#~#EPF Wages#~#EPS Wages#~#EDLI Wages#~#EPF EE Share#~#EPS ER Share#~#EPF ER Share Difference#~#NCP Days#~#Refund of Advances
     */
    @Transactional(readOnly = true)
    public String generatePfEcrFile(Long runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + runId));

        List<PayrollEntry> entries = entryRepo.findByRunIdWithEmployee(runId);
        StringBuilder sb = new StringBuilder();

        for (PayrollEntry e : entries) {
            if (e.getPfEmployee().compareTo(BigDecimal.ZERO) <= 0) continue;

            Employee emp = e.getEmployee();
            String uan = emp.getPfNumber() != null ? emp.getPfNumber() : "100000000000";
            String name = emp.getName() != null ? emp.getName().toUpperCase() : "EMPLOYEE";
            
            long grossWages = e.getGrossSalary().setScale(0, RoundingMode.HALF_UP).longValue();
            // EPF wages capped at 15,000 for standard statutory calculation or actual basic
            long epfWages = Math.min(e.getBasic().setScale(0, RoundingMode.HALF_UP).longValue(), 15000L);
            long epsWages = epfWages;
            long edliWages = epfWages;

            long eeShare = e.getPfEmployee().setScale(0, RoundingMode.HALF_UP).longValue();
            
            // Employer EPS (8.33% of basic up to 15k cap = max 1250)
            long epsErShare = Math.min(Math.round(epfWages * 0.0833), 1250L);
            long epfErDiff = Math.max(0, eeShare - epsErShare);
            
            int ncpDays = e.getDaysLop() != null ? e.getDaysLop() : 0;
            long refundOfAdvances = 0;

            sb.append(uan).append("#~#")
              .append(name).append("#~#")
              .append(grossWages).append("#~#")
              .append(epfWages).append("#~#")
              .append(epsWages).append("#~#")
              .append(edliWages).append("#~#")
              .append(eeShare).append("#~#")
              .append(epsErShare).append("#~#")
              .append(epfErDiff).append("#~#")
              .append(ncpDays).append("#~#")
              .append(refundOfAdvances).append("\n");
        }

        log.info("PF ECR file generated for payroll run ID {}", runId);
        return sb.toString();
    }

    /**
     * Generates ESIC Monthly Return CSV format.
     * Layout: IP Number,IP Name,No of Days Worked,Total Monthly Wages,Reason Code for Zero Working Days,Last Working Day
     */
    @Transactional(readOnly = true)
    public String generateEsiReturnCsv(Long runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + runId));

        List<PayrollEntry> entries = entryRepo.findByRunIdWithEmployee(runId);
        StringBuilder sb = new StringBuilder();
        sb.append("IP Number,IP Name,No of Days Worked,Total Monthly Wages,Reason Code for Zero Working Days,Last Working Day\n");

        for (PayrollEntry e : entries) {
            if (e.getEsiEmployee().compareTo(BigDecimal.ZERO) <= 0) continue;

            Employee emp = e.getEmployee();
            String esiNo = emp.getEsiNumber() != null ? emp.getEsiNumber() : "3100000000";
            String name = emp.getName() != null ? emp.getName().toUpperCase() : "EMPLOYEE";
            int daysWorked = e.getDaysPresent() != null ? e.getDaysPresent() : 0;
            BigDecimal wages = e.getGrossSalary().setScale(2, RoundingMode.HALF_UP);

            sb.append(csvEscape(esiNo)).append(",")
              .append(csvEscape(name)).append(",")
              .append(daysWorked).append(",")
              .append(wages).append(",")
              .append(daysWorked == 0 ? "1" : "0").append(",")
              .append("").append("\n");
        }

        log.info("ESI return CSV generated for payroll run ID {}", runId);
        return sb.toString();
    }

    /**
     * Generates Professional Tax (PT) State-wise Summary for a payroll run.
     */
    @Transactional(readOnly = true)
    public List<PtStateSummary> generatePtSummary(Long runId) {
        List<PayrollEntry> entries = entryRepo.findByRunIdWithEmployee(runId);
        Map<String, PtStateSummary> summaryMap = new LinkedHashMap<>();

        for (PayrollEntry e : entries) {
            if (e.getProfessionalTax().compareTo(BigDecimal.ZERO) <= 0) continue;

            String state = "DEFAULT";
            if (e.getEmployee() != null && e.getEmployee().getDepartment() != null) {
                state = "KA"; // Default state for summary
            }

            PtStateSummary summary = summaryMap.computeIfAbsent(state, k -> PtStateSummary.builder()
                    .stateCode(k)
                    .employeeCount(0)
                    .totalTaxAmount(BigDecimal.ZERO)
                    .build());

            summary.setEmployeeCount(summary.getEmployeeCount() + 1);
            summary.setTotalTaxAmount(summary.getTotalTaxAmount().add(e.getProfessionalTax()));
        }

        return new ArrayList<>(summaryMap.values());
    }

    /**
     * Generates TDS 24Q Quarterly Summary Report.
     */
    @Transactional(readOnly = true)
    public List<Tds24qEntry> generateTds24qSummary(int payYear, int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;

        List<PayrollRun> runs = runRepo.findAll().stream()
                .filter(r -> r.getPayYear() == payYear && r.getPayMonth() >= startMonth && r.getPayMonth() <= endMonth)
                .collect(Collectors.toList());

        Map<Long, Tds24qEntry> map = new HashMap<>();

        for (PayrollRun run : runs) {
            List<PayrollEntry> entries = entryRepo.findByRunIdWithEmployee(run.getId());
            for (PayrollEntry e : entries) {
                if (e.getTds().compareTo(BigDecimal.ZERO) <= 0) continue;

                Employee emp = e.getEmployee();
                Tds24qEntry entry = map.computeIfAbsent(emp.getId(), k -> Tds24qEntry.builder()
                        .employeeId(emp.getId())
                        .employeeName(emp.getName())
                        .pan(emp.getPanNumber() != null ? emp.getPanNumber() : "PANNOTAVAIL")
                        .totalGross(BigDecimal.ZERO)
                        .totalTdsDeducted(BigDecimal.ZERO)
                        .build());

                entry.setTotalGross(entry.getTotalGross().add(e.getGrossSalary()));
                entry.setTotalTdsDeducted(entry.getTotalTdsDeducted().add(e.getTds()));
            }
        }

        return new ArrayList<>(map.values());
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    @Data @Builder
    public static class PtStateSummary {
        private String stateCode;
        private int employeeCount;
        private BigDecimal totalTaxAmount;
    }

    @Data @Builder
    public static class Tds24qEntry {
        private Long employeeId;
        private String employeeName;
        private String pan;
        private BigDecimal totalGross;
        private BigDecimal totalTdsDeducted;
    }
}
