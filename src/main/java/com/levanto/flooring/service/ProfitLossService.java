package com.levanto.flooring.service;

import com.levanto.flooring.repository.ExpenseRepository;
import com.levanto.flooring.repository.IncomeTransactionRepository;
import com.levanto.flooring.repository.EmployeeSalaryRepository;
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
        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            LocalDate from  = month.withDayOfMonth(1);
            LocalDate to    = month.withDayOfMonth(month.lengthOfMonth());
            result.add(calculate(from, to));
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
