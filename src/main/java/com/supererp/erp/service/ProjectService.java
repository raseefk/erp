package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository    projectRepo;
    private final JobCardRepository    jobCardRepo;
    private final ProjectExpenseRepository expRepo;
    private final DailyLogRepository   logRepo;
    private final ProjectLabourRepository labourRepo;
    private final DailyLabourLogRepository dailyLabourRepo;

    // ── Projects ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<com.supererp.erp.projection.ProjectSummary> getAll(int page, int size, String q) {
        Pageable pg = PageRequest.of(page, size, Sort.by("id").descending());
        return projectRepo.searchSummaries(q != null ? q.trim() : null, pg);
    }

    @Transactional(readOnly = true)
    public List<Project> getActive() {
        return projectRepo.findByStatusOrderByCreatedAtDesc(ProjectStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Project getById(Long id) {
        return projectRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }

    @Transactional
    public Project saveProject(Project p) { return projectRepo.save(p); }

    @Transactional
    public void deleteProject(Long id) {
        if (expRepo.sumByProjectAndStatus(id, ProjectExpenseStatus.APPROVED).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete because an associated expense has already been approved.");
        }
        projectRepo.deleteById(id);
    }

    // ── Job Cards ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<JobCard> getJobCards(Long projectId) {
        return jobCardRepo.findByProjectOrderByCreatedAtDesc(getById(projectId));
    }

    @Transactional(readOnly = true)
    public JobCard getJobCard(Long id) {
        return jobCardRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Job card not found: " + id));
    }

    @Transactional
    public JobCard saveJobCard(JobCard jc) { return jobCardRepo.save(jc); }

    @Transactional
    public void deleteJobCard(Long id) {
        if (expRepo.countByJobCard_IdAndStatus(id, ProjectExpenseStatus.APPROVED) > 0) {
            throw new IllegalStateException("Cannot delete because an associated expense has already been approved.");
        }
        jobCardRepo.deleteById(id);
    }

    // ── Project Labours ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProjectLabour> getProjectLabours(Long projectId) {
        return labourRepo.findByProjectIdOrderByNameAsc(projectId);
    }

    @Transactional(readOnly = true)
    public ProjectLabour getProjectLabour(Long id) {
        return labourRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project labour not found: " + id));
    }

    @Transactional
    public ProjectLabour saveProjectLabour(ProjectLabour labour) {
        return labourRepo.save(labour);
    }

    @Transactional
    public void deleteProjectLabour(Long id) {
        labourRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DailyLabourLog> getDailyLabourLogs(Long projectLabourId) {
        return dailyLabourRepo.findByProjectLabourIdOrderByDailyLogLogDateDesc(projectLabourId);
    }

    @Transactional(readOnly = true)
    public List<DailyLabourLog> getApprovedFilteredLabourWages(Long projectId, java.time.LocalDate from, java.time.LocalDate to, String labourName) {
        return dailyLabourRepo.findApprovedFiltered(projectId, from, to, labourName);
    }

    // ── Project analytics ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public BigDecimal totalWorkValue(Long projectId) {
        return logRepo.totalWorkValueByProject(projectId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalApprovedExpenses(Long projectId) {
        return expRepo.sumByProjectAndStatus(projectId, ProjectExpenseStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPendingExpenses(Long projectId) {
        return expRepo.sumByProjectAndStatus(projectId, ProjectExpenseStatus.NEW);
    }

    /** Profitability = workValue − approvedExpenses */
    @Transactional(readOnly = true)
    public BigDecimal netProfitability(Long projectId) {
        return totalWorkValue(projectId).subtract(totalApprovedExpenses(projectId));
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return projectRepo.countByStatus(ProjectStatus.ACTIVE);
    }
}
