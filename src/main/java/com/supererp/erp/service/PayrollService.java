package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.PayrollRunStatus;
import com.supererp.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Core Payroll & Compliance Engine.
 *
 * Key design decisions:
 * - All statutory deductions (PF/ESI/PT/TDS) are OPTIONAL per PayrollConfig.
 * - Computation is purely functional: same inputs → same outputs.
 * - Gross is pro-rated for LOP days using calendar working-day count.
 * - Pending arrears are auto-included in the next run.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private final PayrollRunRepository       runRepo;
    private final PayrollEntryRepository     entryRepo;
    private final PayrollConfigRepository    configRepo;
    private final PayrollArrearRepository    arrearRepo;
    private final EmployeeRepository         employeeRepo;
    private final AttendanceRepository       attendanceRepo;
    private final LeaveApplicationRepository leaveRepo;
    private final LeaveBalanceRepository     leaveBalanceRepo;
    private final HolidayRepository          holidayRepo;
    private final CompanySettingsService     settingsService;
    private final ExpenseRepository          expenseRepo;
    private final EmployeeSalaryRepository   employeeSalaryRepo;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    // ── Professional Tax slabs (state-wise, monthly gross → PT) ──────────────
    // Source: standard Indian PT slabs.  Extend as required.
    private static final Map<String, List<long[]>> PT_SLABS = buildPtSlabs();

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYROLL CONFIG
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PayrollConfig getOrDefaultConfig(Long employeeId) {
        return configRepo.findByEmployeeId(employeeId).orElseGet(() -> {
            Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
            return PayrollConfig.builder().employee(emp).build(); // defaults applied by @Builder.Default
        });
    }

    @Transactional
    public PayrollConfig saveConfig(Long employeeId, PayrollConfig config) {
        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        PayrollConfig existing = configRepo.findByEmployeeId(employeeId).orElse(config);
        existing.setEmployee(emp);
        existing.setBasicPct(config.getBasicPct());
        existing.setHraPct(config.getHraPct());
        existing.setDaPct(config.getDaPct());
        existing.setPfEnabled(config.isPfEnabled());
        existing.setPfPct(config.getPfPct());
        existing.setEsiEnabled(config.isEsiEnabled());
        existing.setPtEnabled(config.isPtEnabled());
        existing.setPtState(config.getPtState());
        existing.setTdsEnabled(config.isTdsEnabled());
        existing.setTdsAnnual(config.getTdsAnnual());
        existing.setEncashableLeaveDays(config.getEncashableLeaveDays());
        return configRepo.save(existing);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYROLL RUN MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<PayrollRun> getAllRuns(int page, int size) {
        return runRepo.findAllOrdered(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public PayrollRun getRunById(Long id) {
        return runRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PayrollEntry> getRunEntries(Long runId) {
        return entryRepo.findByRunIdWithEmployee(runId);
    }

    @Transactional(readOnly = true)
    public PayrollEntry getEntry(Long entryId) {
        return entryRepo.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Payroll entry not found: " + entryId));
    }

    @Transactional(readOnly = true)
    public List<PayrollEntry> getEmployeeHistory(Long employeeId) {
        return entryRepo.findByEmployeeIdOrdered(employeeId);
    }

    /**
     * Create (or regenerate) a payroll run for the given month.
     * Computes every active employee's payslip.
     * Safe to call multiple times — DRAFT runs are regenerated in place.
     */
    @Transactional
    public PayrollRun generateRun(int month, int year, String createdBy) {
        // Check for existing run
        Optional<PayrollRun> existing = runRepo.findByPayMonthAndPayYear(month, year);
        if (existing.isPresent()) {
            PayrollRun run = existing.get();
            if (run.getStatus() != PayrollRunStatus.DRAFT) {
                throw new IllegalStateException("Cannot regenerate an " + run.getStatus() + " payroll run.");
            }
            // Remove old entries and recompute
            run.getEntries().clear();
            runRepo.save(run); // flush deletions
            return computeRun(run, month, year, createdBy);
        }

        String label = LocalDate.of(year, month, 1).format(MONTH_FMT);
        PayrollRun run = PayrollRun.builder()
            .payMonth(month)
            .payYear(year)
            .payPeriodLabel(label)
            .status(PayrollRunStatus.DRAFT)
            .build();
        run = runRepo.save(run);
        return computeRun(run, month, year, createdBy);
    }

    private PayrollRun computeRun(PayrollRun run, int month, int year, String createdBy) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd   = ym.atEndOfMonth();

        List<Employee> employees = employeeRepo.findByActiveTrueOrderByNameAsc();
        CompanySettings settings = settingsService.getSettings();
        List<java.time.DayOfWeek> weekends = settings.getWeeklyOffDaysList();
        List<Holiday> holidays = holidayRepo.findByDateBetween(periodStart, periodEnd);

        // Count total working days for the month
        int totalWorkingDays = countWorkingDays(periodStart, periodEnd, weekends, holidays);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (Employee emp : employees) {
            PayrollEntry entry = computeEntry(emp, run, month, year, periodStart, periodEnd,
                    totalWorkingDays, weekends, holidays);
            run.getEntries().add(entry);
            entry.setPayrollRun(run);
            totalGross       = totalGross.add(entry.getGrossSalary());
            totalDeductions  = totalDeductions.add(entry.getTotalDeductions());
            totalNet         = totalNet.add(entry.getNetSalary());
        }

        run.setTotalGross(totalGross);
        run.setTotalDeductions(totalDeductions);
        run.setTotalNet(totalNet);
        PayrollRun saved = runRepo.save(run);
        log.info("Payroll run generated for {}/{} — {} employees — total net ₹{}",
            month, year, employees.size(), totalNet);
        return saved;
    }

    private PayrollEntry computeEntry(Employee emp, PayrollRun run,
                                       int month, int year,
                                       LocalDate periodStart, LocalDate periodEnd,
                                       int totalWorkingDays,
                                       List<java.time.DayOfWeek> weekends,
                                       List<Holiday> holidays) {

        PayrollConfig cfg = configRepo.findByEmployeeId(emp.getId())
            .orElseGet(() -> PayrollConfig.builder().employee(emp).build());

        // ── Attendance summary ────────────────────────────────────────────────
        List<Attendance> attendances = attendanceRepo
            .findByDateBetweenAndEmployeeIdOptional(periodStart, periodEnd, emp.getId());
        List<LeaveApplication> approvedLeaves = leaveRepo
            .findApprovedLeavesInPeriod(emp.getId(), periodStart, periodEnd);

        int daysPresent = 0, daysLeave = 0, daysLop = 0;
        for (LocalDate d = periodStart; !d.isAfter(periodEnd); d = d.plusDays(1)) {
            final LocalDate cur = d;
            if (weekends.contains(cur.getDayOfWeek())) continue;
            if (holidays.stream().anyMatch(h -> h.getDate().equals(cur))) continue;

            Attendance att = attendances.stream().filter(a -> a.getDate().equals(cur)).findFirst().orElse(null);
            boolean onLeave = approvedLeaves.stream()
                .anyMatch(l -> !cur.isBefore(l.getStartDate()) && !cur.isAfter(l.getEndDate()));

            if (att != null && att.getStatus() != null) {
                if (att.getStatus() == Attendance.AttendanceStatus.PRESENT
                        || att.getStatus() == Attendance.AttendanceStatus.HALF_DAY) {
                    daysPresent++;
                }
            } else if (onLeave) {
                // Check if LOP
                boolean isLop = approvedLeaves.stream()
                    .filter(l -> !cur.isBefore(l.getStartDate()) && !cur.isAfter(l.getEndDate()))
                    .anyMatch(l -> l.getLeaveType() == LeaveApplication.LeaveType.LOSS_OF_PAY);
                if (isLop) daysLop++; else daysLeave++;
            } else {
                daysLop++; // absent without leave = LOP
            }
        }
        int daysAbsent = totalWorkingDays - daysPresent - daysLeave - daysLop;
        if (daysAbsent < 0) daysAbsent = 0;

        // ── CTC and gross ─────────────────────────────────────────────────────
        BigDecimal ctcMonthly = emp.getMonthlySalary() != null ? emp.getMonthlySalary() : BigDecimal.ZERO;

        // Pro-rate for LOP
        BigDecimal grossBeforeLop = ctcMonthly;
        if (daysLop > 0 && totalWorkingDays > 0) {
            BigDecimal perDay = ctcMonthly.divide(BigDecimal.valueOf(totalWorkingDays), 4, RoundingMode.HALF_UP);
            BigDecimal lopDeduction = perDay.multiply(BigDecimal.valueOf(daysLop)).setScale(2, RoundingMode.HALF_UP);
            grossBeforeLop = ctcMonthly.subtract(lopDeduction).max(BigDecimal.ZERO);
        }

        // ── CTC Breakup ───────────────────────────────────────────────────────
        BigDecimal pct = new BigDecimal("100");
        BigDecimal basic   = grossBeforeLop.multiply(cfg.getBasicPct()).divide(pct, 2, RoundingMode.HALF_UP);
        BigDecimal hra     = grossBeforeLop.multiply(cfg.getHraPct()).divide(pct, 2, RoundingMode.HALF_UP);
        BigDecimal da      = grossBeforeLop.multiply(cfg.getDaPct()).divide(pct, 2, RoundingMode.HALF_UP);
        BigDecimal special = grossBeforeLop.subtract(basic).subtract(hra).subtract(da).max(BigDecimal.ZERO);

        // ── Arrears ───────────────────────────────────────────────────────────
        BigDecimal arrearTotal = arrearRepo.sumUnpaidByEmployee(emp.getId());
        if (arrearTotal == null) arrearTotal = BigDecimal.ZERO;

        BigDecimal grossSalary = grossBeforeLop.add(arrearTotal);

        // ── Statutory deductions (all optional) ───────────────────────────────
        BigDecimal pfEmployee  = BigDecimal.ZERO;
        BigDecimal pfEmployer  = BigDecimal.ZERO;
        BigDecimal esiEmployee = BigDecimal.ZERO;
        BigDecimal esiEmployer = BigDecimal.ZERO;
        BigDecimal pt          = BigDecimal.ZERO;
        BigDecimal tds         = BigDecimal.ZERO;

        if (cfg.isPfEnabled()) {
            BigDecimal pfBase = basic; // PF on basic
            BigDecimal pfPct  = cfg.getPfPct() != null ? cfg.getPfPct() : new BigDecimal("12");
            pfEmployee = pfBase.multiply(pfPct).divide(pct, 2, RoundingMode.HALF_UP);
            pfEmployer = pfEmployee; // employer contribution same rate
        }

        if (cfg.isEsiEnabled() && grossBeforeLop.compareTo(new BigDecimal("21000")) <= 0) {
            // ESI applicable only if gross <= ₹21,000
            esiEmployee = grossBeforeLop.multiply(new BigDecimal("0.75")).divide(pct, 2, RoundingMode.HALF_UP);
            esiEmployer = grossBeforeLop.multiply(new BigDecimal("3.25")).divide(pct, 2, RoundingMode.HALF_UP);
        }

        if (cfg.isPtEnabled()) {
            pt = computePt(cfg.getPtState(), grossBeforeLop);
        }

        if (cfg.isTdsEnabled() && cfg.getTdsAnnual() != null
                && cfg.getTdsAnnual().compareTo(BigDecimal.ZERO) > 0) {
            tds = cfg.getTdsAnnual().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalDeductions = pfEmployee.add(esiEmployee).add(pt).add(tds);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions).max(BigDecimal.ZERO);

        return PayrollEntry.builder()
            .payrollRun(run)
            .employee(emp)
            .totalWorkingDays(totalWorkingDays)
            .daysPresent(daysPresent)
            .daysAbsent(daysAbsent)
            .daysLeave(daysLeave)
            .daysLop(daysLop)
            .ctcMonthly(ctcMonthly)
            .grossSalary(grossSalary)
            .basic(basic)
            .hra(hra)
            .da(da)
            .specialAllowance(special)
            .arrears(arrearTotal)
            .pfEmployee(pfEmployee)
            .pfEmployer(pfEmployer)
            .esiEmployee(esiEmployee)
            .esiEmployer(esiEmployer)
            .professionalTax(pt)
            .tds(tds)
            .totalDeductions(totalDeductions)
            .netSalary(netSalary)
            .bankName(emp.getBankName())
            .accountNumber(emp.getAccountNumber())
            .ifscCode(emp.getIfscCode())
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // APPROVAL WORKFLOW
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollRun approveRun(Long runId, String approvedBy) {
        PayrollRun run = getRunById(runId);
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT runs can be approved.");
        }
        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedBy(approvedBy);
        run.setApprovedAt(java.time.LocalDateTime.now());
        log.info("Payroll run {} approved by {}", runId, approvedBy);
        return runRepo.save(run);
    }

    @Transactional
    public PayrollRun cancelRun(Long runId) {
        PayrollRun run = getRunById(runId);
        if (run.getStatus() == PayrollRunStatus.DISBURSED) {
            throw new IllegalStateException("Cannot cancel a disbursed payroll run.");
        }
        run.setStatus(PayrollRunStatus.CANCELLED);
        return runRepo.save(run);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ARREARS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollArrear createArrear(Long employeeId, String period,
                                       BigDecimal oldSalary, BigDecimal newSalary,
                                       BigDecimal amount, String reason) {
        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        PayrollArrear arrear = PayrollArrear.builder()
            .employee(emp)
            .arrearPeriod(period)
            .oldSalary(oldSalary)
            .newSalary(newSalary)
            .arrearAmount(amount)
            .reason(reason)
            .paid(false)
            .build();
        return arrearRepo.save(arrear);
    }

    @Transactional(readOnly = true)
    public List<PayrollArrear> getUnpaidArrears() {
        return arrearRepo.findAllUnpaid();
    }

    /** Mark arrears as paid after they've been included in a run entry */
    @Transactional
    public void markArrearsAsPaid(Long employeeId, PayrollEntry payrollEntry) {
        List<PayrollArrear> unpaid = arrearRepo.findUnpaidByEmployee(employeeId);
        unpaid.forEach(a -> {
            a.setPaid(true);
            a.setPayrollEntry(payrollEntry);
        });
        arrearRepo.saveAll(unpaid);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEAVE ENCASHMENT AT YEAR-END
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Compute and queue leave encashment arrears for all employees at year-end.
     * Call this in December or at year-close. Encashable leaves are per PayrollConfig.
     */
    @Transactional
    public int processLeaveEncashments(int year) {
        List<Employee> employees = employeeRepo.findByActiveTrueOrderByNameAsc();
        int count = 0;
        for (Employee emp : employees) {
            PayrollConfig cfg = configRepo.findByEmployeeId(emp.getId()).orElse(null);
            if (cfg == null) continue;

            int maxEncashableConfig = cfg.getEncashableLeaveDays() != null ? cfg.getEncashableLeaveDays() : 0;
            int remainingCasualLeaves = leaveBalanceRepo.findByEmployeeIdAndYear(emp.getId(), year)
                .map(LeaveBalance::getRemainingCasualLeaves)
                .orElse(maxEncashableConfig);

            int actualEncashableDays = Math.min(maxEncashableConfig, Math.max(0, remainingCasualLeaves));
            if (actualEncashableDays <= 0) continue;

            BigDecimal perDay = emp.getMonthlySalary()
                .divide(BigDecimal.valueOf(26), 4, RoundingMode.HALF_UP); // 26 working days standard
            BigDecimal encashAmount = perDay
                .multiply(BigDecimal.valueOf(actualEncashableDays))
                .setScale(2, RoundingMode.HALF_UP);

            if (encashAmount.compareTo(BigDecimal.ZERO) > 0) {
                createArrear(emp.getId(), "Leave Encashment " + year,
                    BigDecimal.ZERO, emp.getMonthlySalary(),
                    encashAmount, "Annual leave encashment (" + actualEncashableDays + " days)");
                count++;
                log.info("Leave encashment queued for {} — {} days — ₹{}", emp.getName(), actualEncashableDays, encashAmount);
            }
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEFT BANK DISBURSEMENT FILE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate NEFT bulk upload CSV string.
     * Format: Sequence, BeneName, BeneAccount, IFSC, Amount, Remarks
     */
    @Transactional
    public String generateNeftFile(Long runId) {
        PayrollRun run = getRunById(runId);
        if (run.getStatus() != PayrollRunStatus.APPROVED
                && run.getStatus() != PayrollRunStatus.DISBURSED) {
            throw new IllegalStateException("NEFT file can only be generated for APPROVED runs.");
        }

        List<PayrollEntry> entries = entryRepo.findByRunIdWithEmployee(runId);
        StringBuilder sb = new StringBuilder();
        sb.append("Seq,Beneficiary Name,Account Number,IFSC Code,Amount,Remarks\n");

        final int[] seqArr = {1}; // Use array to make it mutable and effectively final
        for (PayrollEntry e : entries) {
            if (e.getNetSalary() == null || e.getNetSalary().compareTo(BigDecimal.ZERO) <= 0) continue;

            String accNum = e.getAccountNumber();
            if (accNum == null || accNum.isBlank()) {
                accNum = e.getEmployee() != null ? e.getEmployee().getAccountNumber() : null;
            }
            if (accNum == null || accNum.isBlank()) {
                accNum = "NO_BANK_ACC_SET";
            }

            String ifsc = e.getIfscCode();
            if (ifsc == null || ifsc.isBlank()) {
                ifsc = e.getEmployee() != null ? e.getEmployee().getIfscCode() : "";
            }
            if (ifsc == null) ifsc = "";

            String name = e.getEmployee() != null ? e.getEmployee().getName() : "Employee #" + e.getId();

            sb.append(seqArr[0]++).append(",")
              .append(csvEscape(name)).append(",")
              .append(csvEscape(accNum)).append(",")
              .append(csvEscape(ifsc)).append(",")
              .append(e.getNetSalary().setScale(2, RoundingMode.HALF_UP)).append(",")
              .append(csvEscape("Salary " + run.getPayPeriodLabel())).append("\n");
        }

        // Mark as DISBURSED
        if (run.getStatus() == PayrollRunStatus.APPROVED) {
            run.setStatus(PayrollRunStatus.DISBURSED);
            run.setDisbursementDate(LocalDate.now());
            // Mark all entries disbursed
            entries.forEach(e -> e.setDisbursed(true));
            entryRepo.saveAll(entries);
            runRepo.save(run);

            // Auto-post payroll expense to Finance module (deducting any individual payouts already posted)
            try {
                String ref = "PAYROLL-RUN-" + run.getId();
                if (!expenseRepo.existsByReference(ref)) {
                    BigDecimal totalGross = run.getTotalGross() != null ? run.getTotalGross() : BigDecimal.ZERO;
                    BigDecimal individualPaid = employeeSalaryRepo.sumAmountBySalaryMonthYear(run.getPayPeriodLabel());
                    if (individualPaid == null) individualPaid = BigDecimal.ZERO;

                    BigDecimal netExpenseToPost = totalGross.subtract(individualPaid).max(BigDecimal.ZERO);
                    if (netExpenseToPost.compareTo(BigDecimal.ZERO) > 0) {
                        Expense salaryExpense = Expense.builder()
                            .category(com.supererp.erp.enums.ExpenseCategory.SALARY)
                            .description("Payroll Disbursement for " + run.getPayPeriodLabel() + " (" + entries.size() + " employees)")
                            .amount(netExpenseToPost)
                            .expenseDate(LocalDate.now())
                            .reference(ref)
                            .build();
                        expenseRepo.save(salaryExpense);
                        log.info("Auto-posted payroll expense of ₹{} (net of ₹{} individual payouts) to Finance for run ID {}", netExpenseToPost, individualPaid, runId);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to auto-post payroll expense to Finance: {}", ex.getMessage());
            }
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private int countWorkingDays(LocalDate start, LocalDate end,
                                  List<java.time.DayOfWeek> weekends, List<Holiday> holidays) {
        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            final LocalDate d = current; // Make it final for lambda
            if (!weekends.contains(d.getDayOfWeek())
                    && holidays.stream().noneMatch(h -> h.getDate().equals(d))) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    private BigDecimal computePt(String state, BigDecimal monthlyGross) {
        if (state == null) return BigDecimal.ZERO;
        List<long[]> slabs = PT_SLABS.getOrDefault(state.toUpperCase(), PT_SLABS.get("DEFAULT"));
        long grossLong = monthlyGross.longValue();
        for (long[] slab : slabs) {
            if (grossLong <= slab[0]) return BigDecimal.valueOf(slab[1]);
        }
        return BigDecimal.ZERO;
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private static Map<String, List<long[]>> buildPtSlabs() {
        Map<String, List<long[]>> m = new HashMap<>();
        // Karnataka slabs (monthly gross → PT per month)
        m.put("KA", Arrays.asList(
            new long[]{14999, 0}, new long[]{21999, 150}, new long[]{Long.MAX_VALUE, 200}));
        // Maharashtra
        m.put("MH", Arrays.asList(
            new long[]{7500, 0}, new long[]{10000, 175}, new long[]{Long.MAX_VALUE, 200}));
        // Tamil Nadu
        m.put("TN", Arrays.asList(
            new long[]{3500, 0}, new long[]{5000, 22}, new long[]{7500, 52},
            new long[]{10000, 115}, new long[]{12500, 125}, new long[]{15000, 145},
            new long[]{20000, 182}, new long[]{Long.MAX_VALUE, 208}));
        // Andhra Pradesh / Telangana
        m.put("AP", Arrays.asList(
            new long[]{15000, 0}, new long[]{20000, 150}, new long[]{Long.MAX_VALUE, 200}));
        m.put("TS", m.get("AP"));
        // West Bengal
        m.put("WB", Arrays.asList(
            new long[]{10000, 0}, new long[]{15000, 110}, new long[]{25000, 130},
            new long[]{40000, 150}, new long[]{Long.MAX_VALUE, 200}));
        // Default (generic) — no PT
        m.put("DEFAULT", List.of(new long[]{Long.MAX_VALUE, 0}));
        return Collections.unmodifiableMap(m);
    }

    // ── Dashboard metrics ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        Page<PayrollRun> recent = runRepo.findAllOrdered(PageRequest.of(0, 6));
        m.put("recentRuns", recent.getContent());
        m.put("totalRuns", recent.getTotalElements());
        m.put("pendingArrears", arrearRepo.findAllUnpaid().size());
        return m;
    }

    @Transactional(readOnly = true)
    public List<PayrollArrear> getEmployeeArrears(Long empId) {
        return arrearRepo.findUnpaidByEmployee(empId);
    }
}
