package com.supererp.erp.service;

import com.supererp.erp.repository.ExpenseRepository;
import com.supererp.erp.repository.IncomeTransactionRepository;
import com.supererp.erp.repository.EmployeeSalaryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfitLossService {

    private final IncomeTransactionRepository incomeRepo;
    private final ExpenseRepository           expenseRepo;
    private final EmployeeSalaryRepository    salaryRepo;

    // ── Single month P&L ─────────────────────────────────────────────────────
    public ProfitLossSummary calculate(LocalDate from, LocalDate to) {
        BigDecimal income   = incomeRepo.sumByDateRange(from, to);
        BigDecimal expenses = expenseRepo.sumByDateRange(from, to);
        BigDecimal salaries = salaryRepo.sumSalaryByDateRange(from, to);
        // Salaries are already included in expenses (via auto-created Expense records)
        // So total cost = expenses (which includes salaries)
        BigDecimal net = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);
        return new ProfitLossSummary(from, to, income, expenses, salaries, net);
    }

    // ── Last 12 months rolling summary ───────────────────────────────────────
    public List<ProfitLossSummary> last12Months() {
        List<ProfitLossSummary> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate start = today.minusMonths(11).withDayOfMonth(1);

        List<Object[]> incomeGrouped = incomeRepo.sumGroupedByMonth(start, end);
        List<Object[]> expenseGrouped = expenseRepo.sumGroupedByMonth(start, end);
        List<Object[]> salaryGrouped = salaryRepo.sumGroupedByMonth(start, end);

        java.util.Map<String, BigDecimal> incomeMap = new java.util.HashMap<>();
        for (Object[] row : incomeGrouped) incomeMap.put(row[0] + "-" + row[1], (BigDecimal) row[2]);

        java.util.Map<String, BigDecimal> expenseMap = new java.util.HashMap<>();
        for (Object[] row : expenseGrouped) expenseMap.put(row[0] + "-" + row[1], (BigDecimal) row[2]);

        java.util.Map<String, BigDecimal> salaryMap = new java.util.HashMap<>();
        for (Object[] row : salaryGrouped) salaryMap.put(row[0] + "-" + row[1], (BigDecimal) row[2]);

        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            LocalDate from  = month.withDayOfMonth(1);
            LocalDate to    = month.withDayOfMonth(month.lengthOfMonth());

            String key = month.getYear() + "-" + month.getMonthValue();
            BigDecimal inc = incomeMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal exp = expenseMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal sal = salaryMap.getOrDefault(key, BigDecimal.ZERO);

            BigDecimal net = inc.subtract(exp).setScale(2, RoundingMode.HALF_UP);
            result.add(new ProfitLossSummary(from, to, inc, exp, sal, net));
        }
        return result;
    }

    // ── Current month quick stats ─────────────────────────────────────────────
    public ProfitLossSummary currentMonth() {
        LocalDate now = LocalDate.now();
        return calculate(now.withDayOfMonth(1), now);
    }

    @Data
    @AllArgsConstructor
    public static class ProfitLossSummary {
        private LocalDate   from;
        private LocalDate   to;
        private BigDecimal  totalIncome;
        private BigDecimal  totalExpenses;
        private BigDecimal  totalSalaries;
        private BigDecimal  netProfit;

        public String getMonthLabel() {
            return from.getMonth().getDisplayName(
                java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                + " " + from.getYear();
        }

        public boolean isProfit() {
            return netProfit != null && netProfit.compareTo(BigDecimal.ZERO) >= 0;
        }
    }
}
