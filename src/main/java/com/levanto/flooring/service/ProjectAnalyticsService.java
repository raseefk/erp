package com.levanto.flooring.service;

import com.levanto.flooring.entity.DailyLog;
import com.levanto.flooring.repository.DailyLogRepository;
import com.levanto.flooring.repository.ProjectExpenseRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectAnalyticsService {

    private final DailyLogRepository       logRepo;
    private final ProjectExpenseRepository expRepo;

    // ── Daily Profit chart data (last 30 days for project) ────────────────────
    public List<DailyProfitPoint> getDailyProfit(Long projectId, int days) {
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        List<DailyLog> logs = logRepo.findByProjectAndDateRange(projectId, from, to);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");

        // Map date → workValue
        Map<LocalDate, BigDecimal> workByDate = new LinkedHashMap<>();
        for (DailyLog l : logs) {
            workByDate.merge(l.getLogDate(),
                l.getWorkValue() != null ? l.getWorkValue() : BigDecimal.ZERO,
                BigDecimal::add);
        }

        List<DailyProfitPoint> points = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            BigDecimal work     = workByDate.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal approved = expRepo.sumApprovedByProjectAndDate(projectId, cursor);
            if (approved == null) approved = BigDecimal.ZERO;
            BigDecimal profit   = work.subtract(approved).setScale(2, RoundingMode.HALF_UP);
            points.add(new DailyProfitPoint(cursor.format(fmt), work, approved, profit));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    // ── Project Pulse (progress bar data) ─────────────────────────────────────
    public ProjectPulse getProjectPulse(Long projectId, BigDecimal contractValue) {
        BigDecimal totalWork  = logRepo.totalWorkValueByProject(projectId);
        BigDecimal totalExp   = expRepo.sumByProjectAndStatus(projectId,
            com.levanto.flooring.enums.ProjectExpenseStatus.APPROVED);
        if (totalWork   == null) totalWork   = BigDecimal.ZERO;
        if (totalExp    == null) totalExp    = BigDecimal.ZERO;
        if (contractValue == null || contractValue.compareTo(BigDecimal.ZERO) == 0)
            contractValue = BigDecimal.ONE;

        // Work completion % vs contract
        int workPct = totalWork.multiply(BigDecimal.valueOf(100))
            .divide(contractValue, 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)).intValue();
        // Spend % vs contract
        int expPct  = totalExp.multiply(BigDecimal.valueOf(100))
            .divide(contractValue, 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)).intValue();

        BigDecimal netProfit = totalWork.subtract(totalExp).setScale(2, RoundingMode.HALF_UP);
        return new ProjectPulse(totalWork, totalExp, netProfit, workPct, expPct);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    @Data @AllArgsConstructor
    public static class DailyProfitPoint {
        private String     label;
        private BigDecimal workValue;
        private BigDecimal expenses;
        private BigDecimal profit;
    }

    @Data @AllArgsConstructor
    public static class ProjectPulse {
        private BigDecimal totalWorkValue;
        private BigDecimal totalApprovedExpenses;
        private BigDecimal netProfit;
        private int        workCompletionPct;
        private int        spendPct;

        public boolean isHealthy() {
            return netProfit != null && netProfit.compareTo(BigDecimal.ZERO) >= 0;
        }
    }
}
