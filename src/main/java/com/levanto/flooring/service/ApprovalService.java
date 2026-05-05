package com.levanto.flooring.service;

import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.*;
import com.levanto.flooring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ProjectExpenseRepository projectExpRepo;
    private final ExpenseRepository        companyExpRepo;

    // ── Approval Queue data ────────────────────────────────────────────────────
    public Page<ProjectExpense> getPendingQueue(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return projectExpRepo.findByStatusOrderByCreatedAtDesc(ProjectExpenseStatus.NEW, pg);
    }

    /** Group pending items by Project for dashboard display */
    public Map<Project, List<ProjectExpense>> getPendingGroupedByProject() {
        List<ProjectExpense> all = projectExpRepo.findByStatusOrderByCreatedAtDesc(ProjectExpenseStatus.NEW);
        return all.stream().collect(
            Collectors.groupingBy(ProjectExpense::getProject,
                LinkedHashMap::new,
                Collectors.toList()));
    }

    public long countPending() {
        return projectExpRepo.countByStatus(ProjectExpenseStatus.NEW);
    }

    // ── APPROVE — transactional bridge ────────────────────────────────────────
    /**
     * 1. Update ProjectExpense.status → APPROVED
     * 2. Insert matching record into CompanyExpense (main ledger)
     * 3. Link ProjectExpense.companyExpense → the new row
     *
     * All three steps in a single @Transactional — either all succeed or all roll back.
     */
    @Transactional
    public Expense approve(Long projectExpenseId, AppUser approvedBy) {
        ProjectExpense pe = getById(projectExpenseId);

        if (pe.getStatus() != ProjectExpenseStatus.NEW) {
            throw new IllegalStateException("Expense is already " + pe.getStatus());
        }

        // Build reference string
        String dateStr = pe.getExpenseDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String reference = "Project: " + pe.getProject().getName()
            + " | Job: "  + pe.getJobCard().getPhase()
            + " | "       + pe.getCategory().name() + " for " + dateStr;

        // Map ProjectExpenseCategory → CompanyExpenseCategory
        ExpenseCategory compCat = mapCategory(pe.getCategory());

        // Insert into main company expense ledger
        Expense companyExpense = Expense.builder()
            .category(compCat)
            .description(pe.getDescription())
            .amount(pe.getAmount())
            .expenseDate(pe.getExpenseDate())
            .reference(reference)
            .attachmentName(pe.getAttachmentName())
            .attachmentPath(pe.getAttachmentPath())
            .attachmentMimeType(pe.getAttachmentMimeType())
            .build();
        companyExpense = companyExpRepo.save(companyExpense);

        // Update bridge record
        pe.setStatus(ProjectExpenseStatus.APPROVED);
        pe.setApprovedBy(approvedBy);
        pe.setApprovedAt(LocalDateTime.now());
        pe.setCompanyExpense(companyExpense);
        projectExpRepo.save(pe);

        if (pe.getCategory() == ProjectExpenseCategory.LABOUR && pe.getDailyLog() != null && pe.getDailyLog().getLabourLogs() != null) {
            pe.getDailyLog().getLabourLogs().forEach(ll -> ll.setStatus(ProjectExpenseStatus.APPROVED));
        }

        log.info("Approved project expense #{}: ₹{} → CompanyExpense #{}",
            pe.getId(), pe.getAmount(), companyExpense.getId());
        return companyExpense;
    }

    // ── REJECT ────────────────────────────────────────────────────────────────
    @Transactional
    public void reject(Long projectExpenseId, AppUser rejectedBy) {
        ProjectExpense pe = getById(projectExpenseId);
        if (pe.getStatus() != ProjectExpenseStatus.NEW) {
            throw new IllegalStateException("Expense is already " + pe.getStatus());
        }
        pe.setStatus(ProjectExpenseStatus.REJECTED);
        pe.setApprovedBy(rejectedBy);
        pe.setApprovedAt(LocalDateTime.now());
        projectExpRepo.save(pe);
        log.info("Rejected project expense #{}", pe.getId());
    }

    // ── BULK APPROVE all NEW for a project ───────────────────────────────────
    @Transactional
    public int approveAllForProject(Long projectId, AppUser approvedBy) {
        List<ProjectExpense> pending = projectExpRepo.findNewByProject(projectId);
        for (ProjectExpense pe : pending) {
            approve(pe.getId(), approvedBy);
        }
        return pending.size();
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public ProjectExpense getById(Long id) {
        return projectExpRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project expense not found: " + id));
    }

    // ── Category mapping ──────────────────────────────────────────────────────
    private ExpenseCategory mapCategory(ProjectExpenseCategory cat) {
        return switch (cat) {
            case LABOUR    -> ExpenseCategory.LABOUR;
            case MATERIAL  -> ExpenseCategory.MATERIAL;
            case FUEL  -> ExpenseCategory.FUEL;
            case TRANSPORT -> ExpenseCategory.TRANSPORT;
            case TOOLS     -> ExpenseCategory.TOOLS;
            case MISC      -> ExpenseCategory.MISC;
            case UTILITY      -> ExpenseCategory.UTILITY;
            case RENT      -> ExpenseCategory.RENT;
        };
    }
}
