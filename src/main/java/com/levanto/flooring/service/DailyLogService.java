package com.levanto.flooring.service;

import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.*;
import com.levanto.flooring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyLogService {

    private final DailyLogRepository       logRepo;
    private final ProjectExpenseRepository expRepo;
    private final JobCardRepository        jobCardRepo;
    private final FileStorageService       fileStorage;

    // ── Save daily log + auto-create Labour ProjectExpense ────────────────────
    @Transactional
    public DailyLog saveLog(DailyLog dailyLog, AppUser submittedBy) {
        // Compute labour cost from itemized logs (or fallback to old summary method)
        BigDecimal labour = BigDecimal.ZERO;
        if (dailyLog.getLabourLogs() != null && !dailyLog.getLabourLogs().isEmpty()) {
            labour = dailyLog.getLabourLogs().stream()
                .map(DailyLabourLog::getWagePaid)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dailyLog.setNumberOfLabours(dailyLog.getLabourLogs().size());
        } else {
            int numLabours = dailyLog.getNumberOfLabours() != null ? dailyLog.getNumberOfLabours() : 0;
            BigDecimal rate = dailyLog.getDailyWageRate() != null ? dailyLog.getDailyWageRate() : BigDecimal.ZERO;
            labour = rate.multiply(BigDecimal.valueOf(numLabours)).setScale(2, RoundingMode.HALF_UP);
        }
        dailyLog.setTotalLabourCost(labour);

        DailyLog saved = logRepo.save(dailyLog);

        // Auto-trigger: create LABOUR ProjectExpense if labour cost > 0
        if (labour.compareTo(BigDecimal.ZERO) > 0) {
            String dateStr = dailyLog.getLogDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String desc = dailyLog.getLabourLogs() != null && !dailyLog.getLabourLogs().isEmpty() ?
                dailyLog.getLabourLogs().stream()
                    .map(ll -> ll.getProjectLabour().getName() + " (" + ll.getWagePaid() + ")")
                    .collect(java.util.stream.Collectors.joining(", ")) :
                dailyLog.getNumberOfLabours() + " Labour(s) @ ₹" + dailyLog.getDailyWageRate() + "/day — " + dateStr;

            if (desc.length() > 295) {
                desc = desc.substring(0, 292) + "...";
            }
            
            ProjectExpense pe = ProjectExpense.builder()
                .dailyLog(saved)
                .project(saved.getJobCard().getProject())
                .jobCard(saved.getJobCard())
                .category(ProjectExpenseCategory.LABOUR)
                .description(desc)
                .amount(labour)
                .expenseDate(dailyLog.getLogDate())
                .status(ProjectExpenseStatus.NEW)
                .submittedBy(submittedBy)
                .build();
            expRepo.save(pe);
            log.info("Auto-created LABOUR expense ₹{} for log #{}", labour, saved.getId());
        }

        return saved;
    }

    // ── Add misc site expense to an existing log ──────────────────────────────
    @Transactional
    public ProjectExpense addMiscExpense(Long logId,
                                         ProjectExpenseCategory category,
                                         String description,
                                         BigDecimal amount,
                                         MultipartFile file,
                                         AppUser submittedBy) throws IOException {
        DailyLog log = getById(logId);

        ProjectExpense pe = ProjectExpense.builder()
            .dailyLog(log)
            .project(log.getJobCard().getProject())
            .jobCard(log.getJobCard())
            .category(category)
            .description(description)
            .amount(amount)
            .expenseDate(log.getLogDate())
            .status(ProjectExpenseStatus.NEW)
            .submittedBy(submittedBy)
            .build();

        if (file != null && !file.isEmpty()) {
            String path = fileStorage.store(file, "project-expenses");
            pe.setAttachmentPath(path);
            pe.setAttachmentName(file.getOriginalFilename());
            pe.setAttachmentMimeType(file.getContentType());
        }

        return expRepo.save(pe);
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public List<DailyLog> getByJobCard(Long jobCardId) {
        JobCard jc = jobCardRepo.findById(jobCardId)
            .orElseThrow(() -> new IllegalArgumentException("Job card not found: " + jobCardId));
        return logRepo.findByJobCardOrderByLogDateDesc(jc);
    }

    public Page<DailyLog> getByProject(Long projectId, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("logDate").descending());
        return logRepo.findByJobCard_Project_IdOrderByLogDateDesc(projectId, pg);
    }

    public DailyLog getById(Long id) {
        return logRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Daily log not found: " + id));
    }

    public List<ProjectExpense> getExpensesForLog(Long logId) {
        DailyLog log = getById(logId);
        return new java.util.ArrayList<>(log.getProjectExpenses());
    }

    // ── Delete (cascades to NEW project expenses via orphanRemoval) ───────────
    @Transactional
    public void deleteLog(Long id) {
        DailyLog dailyLog = getById(id);
        // Safety: block deletion if any expenses already approved
        boolean hasApproved = dailyLog.getProjectExpenses().stream()
            .anyMatch(e -> e.getStatus() == ProjectExpenseStatus.APPROVED);
        if (hasApproved) {
            throw new IllegalStateException(
                "Cannot delete log — it has approved expenses linked to the company ledger.");
        }
        logRepo.delete(dailyLog); // orphanRemoval purges all NEW expenses
        log.info("Deleted daily log #{} and its {} pending expenses",
            id, dailyLog.getProjectExpenses().size());
    }

    // ── Delete individual project expense (only if NEW) ───────────────────────
    @Transactional
    public void deleteMiscExpense(Long expenseId) {
        ProjectExpense pe = expRepo.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + expenseId));
        if (pe.getStatus() != ProjectExpenseStatus.NEW) {
            throw new IllegalStateException("Only NEW expenses can be deleted.");
        }
        if (pe.getAttachmentPath() != null) fileStorage.delete(pe.getAttachmentPath());
        expRepo.delete(pe);
    }
}
