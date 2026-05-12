package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.repository.*;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConstructionManagementService {

    private final BillOfQuantityRepository boqRepo;
    private final BoqItemRepository boqItemRepo;
    private final BoqProgressEntryRepository progressRepo;
    private final SubcontractorRunningBillRepository runningBillRepo;
    private final SubcontractorRunningBillItemRepository runningBillItemRepo;
    private final MaterialSiteTransactionRepository materialRepo;
    private final ProjectMilestoneRepository milestoneRepo;
    private final JobCardRepository jobCardRepo;
    private final PdfService pdfService;
    private final ProjectExpenseRepository expenseRepo;
    private final ProjectRepository projectRepo;
    private final VendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public Page<BillOfQuantity> searchBoqs(String q, int page, int size) {
        return boqRepo.search(q != null ? q.trim() : null, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public BillOfQuantity getBoq(Long id) {
        return boqRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("BOQ not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BillOfQuantity> getBoqsByProject(Long projectId) {
        return boqRepo.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional
    public BillOfQuantity saveBoq(BillOfQuantity boq) {
        if (boq.getTenantId() == null) boq.setTenantId(TenantContext.getTenantId());
        boqRepo.findByTenantIdAndBoqNumber(TenantContext.getTenantId(), boq.getBoqNumber()).ifPresent(existing -> {
            if (boq.getId() == null || !existing.getId().equals(boq.getId())) {
                throw new IllegalArgumentException("BOQ number already exists: " + boq.getBoqNumber());
            }
        });
        return boqRepo.save(boq);
    }

    @Transactional
    public BoqItem saveBoqItem(BoqItem item) {
        if (item.getTenantId() == null) item.setTenantId(TenantContext.getTenantId());
        if (item.getProject() == null && item.getBoq() != null) {
            item.setProject(item.getBoq().getProject());
        }
        return boqItemRepo.save(item);
    }

    @Transactional(readOnly = true)
    public BoqItem getBoqItem(Long id) {
        return boqItemRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("BOQ Item not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BoqItem> getBoqItemsByProject(Long projectId) {
        return boqItemRepo.findByProjectIdOrderByDescriptionAsc(projectId);
    }

    @Transactional
    public BillOfQuantity approveBoq(Long boqId) {
        BillOfQuantity boq = getBoq(boqId);
        boq.setStatus(BoqStatus.APPROVED);
        boq.setApprovedAt(LocalDateTime.now());
        return boqRepo.save(boq);
    }

    @Transactional(readOnly = true)
    public List<BoqItem> itemsForBoq(Long boqId) {
        return boqItemRepo.findByBoqIdOrderByIdAsc(boqId);
    }

    @Transactional
    public BoqProgressEntry recordProgress(Long boqItemId, BigDecimal completedQuantity, java.time.LocalDate progressDate,
                                           Long jobCardId, String remarks, AppUser recordedBy) {
        BoqItem item = boqItemRepo.findById(boqItemId)
            .orElseThrow(() -> new IllegalArgumentException("BOQ item not found: " + boqItemId));
        BigDecimal newCompleted = value(item.getCompletedQuantity()).add(value(completedQuantity));
        if (newCompleted.compareTo(value(item.getQuantity())) > 0) {
            throw new IllegalArgumentException("Completion cannot exceed BOQ quantity.");
        }
        item.setCompletedQuantity(newCompleted);
        updateBoqItemStatus(item);
        boqItemRepo.save(item);

        BoqProgressEntry entry = BoqProgressEntry.builder()
            .tenantId(item.getTenantId())
            .boqItem(item)
            .project(item.getProject())
            .jobCard(jobCardId != null ? jobCardRepo.findById(jobCardId).orElse(null) : null)
            .progressDate(progressDate != null ? progressDate : java.time.LocalDate.now())
            .completedQuantity(value(completedQuantity))
            .remarks(remarks)
            .recordedBy(recordedBy)
            .build();
        return progressRepo.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<SubcontractorRunningBill> searchRunningBills(String q, int page, int size) {
        return runningBillRepo.search(q != null ? q.trim() : null, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public SubcontractorRunningBill getRunningBill(Long id) {
        return runningBillRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Running bill not found: " + id));
    }

    @Transactional
    public SubcontractorRunningBill saveRunningBill(SubcontractorRunningBill bill) {
        if (bill.getTenantId() == null) bill.setTenantId(TenantContext.getTenantId());
        runningBillRepo.findByTenantIdAndBillNumber(TenantContext.getTenantId(), bill.getBillNumber()).ifPresent(existing -> {
            if (bill.getId() == null || !existing.getId().equals(bill.getId())) {
                throw new IllegalArgumentException("Running bill number already exists: " + bill.getBillNumber());
            }
        });
        return runningBillRepo.save(bill);
    }

    @Transactional
    public SubcontractorRunningBillItem saveRunningBillItem(SubcontractorRunningBillItem item) {
        if (item.getTenantId() == null) item.setTenantId(TenantContext.getTenantId());
        return runningBillItemRepo.save(item);
    }

    @Transactional
    public SubcontractorRunningBill createRunningBill(Long projectId, Long vendorId, String billNumber, java.time.LocalDate billDate, java.time.LocalDate periodFrom, java.time.LocalDate periodTo, Long jobCardId) {
        Project project = projectRepo.findById(projectId).orElseThrow();
        Vendor vendor = vendorRepo.findById(vendorId).orElseThrow();
        
        SubcontractorRunningBill bill = SubcontractorRunningBill.builder()
            .project(project)
            .vendor(vendor)
            .billNumber(billNumber)
            .billDate(billDate)
            .periodFrom(periodFrom)
            .periodTo(periodTo)
            .tenantId(TenantContext.getTenantId())
            .build();
            
        if (jobCardId != null) {
            bill.setJobCard(jobCardRepo.findById(jobCardId).orElse(null));
        }
        
        return runningBillRepo.save(bill);
    }

    @Transactional
    public SubcontractorRunningBill submitRunningBill(Long id, AppUser submittedBy) {
        SubcontractorRunningBill bill = getRunningBill(id);
        bill.setSubmittedBy(submittedBy);
        bill.setStatus(SubcontractorBillStatus.SUBMITTED);
        recalculateRunningBillTotals(bill);
        return runningBillRepo.save(bill);
    }

    @Transactional
    public SubcontractorRunningBill certifyRunningBill(Long id, Map<Long, BigDecimal> certifiedQuantities, AppUser certifiedBy) {
        SubcontractorRunningBill bill = getRunningBill(id);
        if (bill.getStatus() != SubcontractorBillStatus.SUBMITTED && bill.getStatus() != SubcontractorBillStatus.DRAFT) {
            throw new IllegalStateException("Only draft/submitted bills can be certified.");
        }
        for (SubcontractorRunningBillItem item : bill.getItems()) {
            BigDecimal certifiedQty = certifiedQuantities.getOrDefault(item.getId(), item.getCertifiedQuantity());
            if (certifiedQty.compareTo(value(item.getClaimedQuantity())) > 0) {
                throw new IllegalArgumentException("Certified quantity cannot exceed claimed quantity for " + item.getDescription());
            }
            item.setCertifiedQuantity(certifiedQty);
            runningBillItemRepo.save(item);
        }
        bill.setCertifiedBy(certifiedBy);
        bill.setCertifiedAt(LocalDateTime.now());
        bill.setStatus(SubcontractorBillStatus.CERTIFIED);
        recalculateRunningBillTotals(bill);
        return runningBillRepo.save(bill);
    }


    @Transactional
    public SubcontractorRunningBill rejectRunningBill(Long id, String reason) {
        SubcontractorRunningBill bill = getRunningBill(id);
        bill.setStatus(SubcontractorBillStatus.REJECTED);
        bill.setRejectionReason(reason);
        return runningBillRepo.save(bill);
    }

    @Transactional
    public SubcontractorRunningBill markRunningBillPaid(Long id) {
        SubcontractorRunningBill bill = getRunningBill(id);
        if (bill.getStatus() != SubcontractorBillStatus.CERTIFIED) {
            throw new IllegalStateException("Only certified bills can be marked paid.");
        }
        bill.setStatus(SubcontractorBillStatus.PAID);

        // Build a rich description with subcontractor and bill details
        StringBuilder desc = new StringBuilder();
        desc.append("Subcontract Payment: ").append(bill.getVendor().getName())
            .append(" [Bill ").append(bill.getBillNumber()).append("]")
            .append(" | Project: ").append(bill.getProject().getName());
        if (bill.getPeriodFrom() != null && bill.getPeriodTo() != null) {
            desc.append(" | Period: ")
                .append(bill.getPeriodFrom().toString())
                .append(" to ")
                .append(bill.getPeriodTo().toString());
        }

        ProjectExpense expense = new ProjectExpense();
        expense.setProject(bill.getProject());
        expense.setTenantId(bill.getTenantId());
        expense.setExpenseDate(bill.getBillDate());
        expense.setCategory(ProjectExpenseCategory.PROJECT_SUBCONTRACT);
        expense.setAmount(bill.getCertifiedAmount());
        expense.setDescription(desc.toString());
        expense.setStatus(ProjectExpenseStatus.NEW); // enters approval queue
        if (bill.getJobCard() != null) {
            expense.setJobCard(bill.getJobCard());
        }
        expenseRepo.save(expense);

        return runningBillRepo.save(bill);
    }

    @Transactional
    public MaterialSiteTransaction recordMaterialTransaction(MaterialSiteTransaction transaction) {
        if (transaction.getTenantId() == null) transaction.setTenantId(TenantContext.getTenantId());
        if (transaction.getTransactionType() == MaterialSiteTransactionType.CONSUMPTION
            || transaction.getTransactionType() == MaterialSiteTransactionType.RETURN) {
            BigDecimal balance = materialBalance(transaction.getProject().getId(), transaction.getInventoryItem().getId());
            if (balance.subtract(value(transaction.getQuantity())).compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Material balance cannot go negative.");
            }
        }
        return materialRepo.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<MaterialSiteTransaction> materialsForProject(Long projectId) {
        return materialRepo.findByProjectIdOrderByTransactionDateDesc(projectId);
    }

    @Transactional(readOnly = true)
    public BigDecimal materialBalance(Long projectId, Long itemId) {
        BigDecimal balance = BigDecimal.ZERO;
        for (MaterialSiteTransaction tx : materialRepo.findByProjectIdAndInventoryItemIdOrderByTransactionDateDesc(projectId, itemId)) {
            BigDecimal qty = value(tx.getQuantity());
            switch (tx.getTransactionType()) {
                case RECEIPT, ADJUSTMENT -> balance = balance.add(qty);
                case CONSUMPTION, RETURN -> balance = balance.subtract(qty);
            }
        }
        return balance;
    }

    @Transactional(readOnly = true)
    public List<JobCard> getActiveJobCards() {
        return jobCardRepo.findAllWithProject();
    }

    @Transactional(readOnly = true)
    public Page<ProjectMilestone> searchMilestones(String q, int page, int size) {
        return milestoneRepo.search(q != null ? q.trim() : null, PageRequest.of(page, size));
    }

    @Transactional
    public ProjectMilestone saveMilestone(ProjectMilestone milestone) {
        if (milestone.getTenantId() == null) milestone.setTenantId(TenantContext.getTenantId());
        
        // If contract amount is not set, inherit from Project
        if (value(milestone.getContractAmount()).compareTo(BigDecimal.ZERO) == 0 && milestone.getProject() != null) {
            projectRepo.findById(milestone.getProject().getId()).ifPresent(p -> {
                milestone.setContractAmount(p.getTotalContractValue());
            });
        }

        if (value(milestone.getReleaseAmount()).compareTo(BigDecimal.ZERO) == 0
            && value(milestone.getContractAmount()).compareTo(BigDecimal.ZERO) > 0) {
            milestone.setReleaseAmount(value(milestone.getContractAmount())
                .multiply(value(milestone.getReleasePercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        return milestoneRepo.save(milestone);
    }

    @Transactional
    public ProjectMilestone submitMilestone(Long id) {
        ProjectMilestone milestone = getMilestone(id);
        
        // Ensure amount is calculated before submission if it's currently 0
        if (value(milestone.getReleaseAmount()).compareTo(BigDecimal.ZERO) == 0) {
            saveMilestone(milestone);
        }

        milestone.setStatus(ProjectMilestoneStatus.SUBMITTED_FOR_CLIENT_APPROVAL);
        milestone.setSubmittedAt(LocalDateTime.now());
        return milestoneRepo.save(milestone);
    }

    @Transactional
    public ProjectMilestone approveMilestone(Long id, String reference) {
        ProjectMilestone milestone = getMilestone(id);
        milestone.setStatus(ProjectMilestoneStatus.CLIENT_APPROVED);
        milestone.setClientApprovedAt(LocalDateTime.now());
        milestone.setClientApprovalReference(reference);
        return milestoneRepo.save(milestone);
    }

    @Transactional
    public ProjectMilestone releaseMilestonePayment(Long id) {
        ProjectMilestone milestone = getMilestone(id);
        if (milestone.getStatus() != ProjectMilestoneStatus.CLIENT_APPROVED) {
            throw new IllegalStateException("Payment can be released only after client approval.");
        }
        milestone.setStatus(ProjectMilestoneStatus.PAYMENT_RELEASED);
        milestone.setPaymentReleasedAt(LocalDateTime.now());
        return milestoneRepo.save(milestone);
    }

    @Transactional(readOnly = true)
    public ProjectMilestone getMilestone(Long id) {
        return milestoneRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + id));
    }

    private void updateBoqItemStatus(BoqItem item) {
        int cmp = value(item.getCompletedQuantity()).compareTo(value(item.getQuantity()));
        if (cmp >= 0) {
            item.setStatus(BoqItemStatus.COMPLETED);
        } else if (value(item.getCompletedQuantity()).compareTo(BigDecimal.ZERO) > 0) {
            item.setStatus(BoqItemStatus.IN_PROGRESS);
        } else {
            item.setStatus(BoqItemStatus.NOT_STARTED);
        }
    }

    private void recalculateRunningBillTotals(SubcontractorRunningBill bill) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal certified = BigDecimal.ZERO;
        for (SubcontractorRunningBillItem item : bill.getItems()) {
            BigDecimal rate = value(item.getRate());
            gross = gross.add(value(item.getClaimedQuantity()).multiply(rate));
            certified = certified.add(value(item.getCertifiedQuantity()).multiply(rate));
        }
        bill.setGrossAmount(gross.setScale(2, RoundingMode.HALF_UP));
        bill.setCertifiedAmount(certified.subtract(value(bill.getDeductionAmount())).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public byte[] generateRunningBillPdf(Long id) {
        SubcontractorRunningBill bill = getRunningBill(id);
        return pdfService.generateSubcontractorBill(bill);
    }

    @Transactional(readOnly = true)
    public byte[] generateMilestonePdf(Long id) {
        ProjectMilestone milestone = getMilestone(id);
        return pdfService.generateMilestonePdf(milestone);
    }

    private BigDecimal value(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

