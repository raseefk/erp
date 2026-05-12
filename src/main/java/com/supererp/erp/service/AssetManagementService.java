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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AssetManagementService {

    private final AssetRepository assetRepo;
    private final AssetDepreciationScheduleRepository depreciationRepo;
    private final AssetAssignmentRepository assignmentRepo;
    private final PreventiveMaintenancePlanRepository maintenancePlanRepo;
    private final AssetMaintenanceJobRepository maintenanceJobRepo;
    private final AssetBreakdownRepository breakdownRepo;
    private final ProjectRepository projectRepo;
    private final JobCardRepository jobCardRepo;
    private final EmployeeRepository employeeRepo;
    private final VendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public Page<Asset> searchAssets(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return assetRepo.search(q != null ? q.trim() : null, pageable);
    }

    @Transactional(readOnly = true)
    public List<Asset> activeAssets() {
        UUID tenantId = TenantContext.getTenantId();
        return assetRepo.findByTenantIdAndStatusOrderByNameAsc(tenantId, AssetStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Asset getAsset(Long id) {
        return assetRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
    }

    @Transactional
    public Asset saveAsset(Asset asset, Long vendorId) {
        UUID tenantId = TenantContext.getTenantId();
        asset.setTenantId(asset.getTenantId() != null ? asset.getTenantId() : tenantId);
        assetRepo.findByTenantIdAndAssetCode(tenantId, asset.getAssetCode()).ifPresent(existing -> {
            if (asset.getId() == null || !existing.getId().equals(asset.getId())) {
                throw new IllegalArgumentException("Asset code already exists: " + asset.getAssetCode());
            }
        });
        if (vendorId != null) {
            asset.setVendor(vendorRepo.findById(vendorId).orElse(null));
        } else {
            asset.setVendor(null);
        }
        if (asset.getCurrentBookValue() == null || asset.getCurrentBookValue().compareTo(BigDecimal.ZERO) == 0) {
            asset.setCurrentBookValue(asset.getPurchaseCost() != null ? asset.getPurchaseCost() : BigDecimal.ZERO);
        }
        return assetRepo.save(asset);
    }

    @Transactional
    public void retireAsset(Long assetId) {
        Asset asset = getAsset(assetId);
        asset.setStatus(AssetStatus.RETIRED);
        assetRepo.save(asset);
    }

    @Transactional(readOnly = true)
    public List<AssetDepreciationSchedule> depreciationSchedule(Long assetId) {
        return depreciationRepo.findByAssetIdOrderByPeriodStartAsc(assetId);
    }

    @Transactional
    public List<AssetDepreciationSchedule> regenerateDepreciationSchedule(Long assetId) {
        Asset asset = getAsset(assetId);
        if (asset.getPurchaseDate() == null) {
            throw new IllegalArgumentException("Purchase date is required to generate depreciation.");
        }
        if (asset.getUsefulLifeMonths() == null || asset.getUsefulLifeMonths() <= 0) {
            throw new IllegalArgumentException("Useful life must be greater than zero.");
        }

        depreciationRepo.deleteByAssetIdAndPostedFalse(assetId);

        BigDecimal purchaseCost = value(asset.getPurchaseCost());
        BigDecimal salvageValue = value(asset.getSalvageValue());
        BigDecimal openingValue = purchaseCost;
        LocalDate periodStart = asset.getPurchaseDate();
        List<AssetDepreciationSchedule> rows = new ArrayList<>();

        for (int i = 0; i < asset.getUsefulLifeMonths(); i++) {
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            BigDecimal depreciation;
            if (asset.getDepreciationMethod() == DepreciationMethod.WDV) {
                BigDecimal annualRate = value(asset.getDepreciationRatePercent()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
                BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
                depreciation = openingValue.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            } else {
                depreciation = purchaseCost.subtract(salvageValue)
                    .divide(BigDecimal.valueOf(asset.getUsefulLifeMonths()), 2, RoundingMode.HALF_UP);
            }

            BigDecimal closingValue = openingValue.subtract(depreciation);
            if (closingValue.compareTo(salvageValue) < 0) {
                depreciation = openingValue.subtract(salvageValue).max(BigDecimal.ZERO);
                closingValue = salvageValue;
            }

            rows.add(AssetDepreciationSchedule.builder()
                .tenantId(asset.getTenantId())
                .asset(asset)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .openingValue(openingValue)
                .depreciationAmount(depreciation)
                .closingValue(closingValue)
                .method(asset.getDepreciationMethod())
                .posted(false)
                .build());

            openingValue = closingValue;
            periodStart = periodStart.plusMonths(1);
            if (openingValue.compareTo(salvageValue) <= 0) break;
        }

        return depreciationRepo.saveAll(rows);
    }

    @Transactional(readOnly = true)
    public Optional<AssetAssignment> currentAssignment(Long assetId) {
        return assignmentRepo.findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedFromDesc(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetAssignment> assignmentHistory(Long assetId) {
        return assignmentRepo.findByAssetIdOrderByAssignedFromDesc(assetId);
    }

    @Transactional
    public AssetAssignment assignAsset(Long assetId, AssetAssignmentType type, Long employeeId, Long projectId, String location, LocalDate assignedFrom, String notes) {
        Asset asset = getAsset(assetId);
        currentAssignment(assetId).ifPresent(active -> {
            throw new IllegalStateException("Asset already has an active assignment.");
        });
        AssetAssignment assignment = AssetAssignment.builder()
            .tenantId(asset.getTenantId())
            .asset(asset)
            .assignmentType(type)
            .employee(employeeId != null ? employeeRepo.findById(employeeId).orElse(null) : null)
            .project(projectId != null ? projectRepo.findById(projectId).orElse(null) : null)
            .location(location)
            .assignedFrom(assignedFrom != null ? assignedFrom : LocalDate.now())
            .notes(notes)
            .build();
        return assignmentRepo.save(assignment);
    }

    @Transactional
    public void returnAsset(Long assignmentId, LocalDate returnedAt) {
        AssetAssignment assignment = assignmentRepo.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Asset assignment not found: " + assignmentId));
        assignment.setReturnedAt(returnedAt != null ? returnedAt : LocalDate.now());
        assignmentRepo.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<PreventiveMaintenancePlan> maintenancePlans(Long assetId) {
        return maintenancePlanRepo.findByAssetIdOrderByNextDueDateAsc(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetMaintenanceJob> maintenanceCalendar(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(7);
        LocalDate end = to != null ? to : LocalDate.now().plusDays(30);
        return maintenanceJobRepo.findByScheduledDateBetweenOrderByScheduledDateAsc(start, end);
    }

    @Transactional
    public PreventiveMaintenancePlan saveMaintenancePlan(Long assetId, MaintenanceFrequency frequency, Integer customIntervalDays,
                                                         LocalDate nextDueDate, Long employeeId, String instructions) {
        Asset asset = getAsset(assetId);
        PreventiveMaintenancePlan plan = PreventiveMaintenancePlan.builder()
            .tenantId(asset.getTenantId())
            .asset(asset)
            .frequency(frequency)
            .customIntervalDays(customIntervalDays)
            .nextDueDate(nextDueDate)
            .assignedEmployee(employeeId != null ? employeeRepo.findById(employeeId).orElse(null) : null)
            .instructions(instructions)
            .active(true)
            .build();
        return maintenancePlanRepo.save(plan);
    }

    @Transactional
    public int generateDueMaintenanceJobs(LocalDate dueDate, boolean createJobCards) {
        List<PreventiveMaintenancePlan> duePlans = maintenancePlanRepo
            .findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(dueDate != null ? dueDate : LocalDate.now());
        int created = 0;
        for (PreventiveMaintenancePlan plan : duePlans) {
            AssetMaintenanceJob job = createMaintenanceJob(plan, createJobCards);
            maintenanceJobRepo.save(job);
            plan.setNextDueDate(nextDueDate(plan));
            maintenancePlanRepo.save(plan);
            created++;
        }
        return created;
    }

    @Transactional
    public AssetMaintenanceJob completeMaintenanceJob(Long jobId, LocalDate completedDate, BigDecimal cost, String notes) {
        AssetMaintenanceJob job = maintenanceJobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Maintenance job not found: " + jobId));
        job.setStatus(MaintenanceJobStatus.COMPLETED);
        job.setCompletedDate(completedDate != null ? completedDate : LocalDate.now());
        job.setCost(value(cost));
        job.setNotes(notes);
        job.getAsset().setStatus(AssetStatus.ACTIVE);
        return maintenanceJobRepo.save(job);
    }

    @Transactional(readOnly = true)
    public List<AssetMaintenanceJob> jobsForAsset(Long assetId) {
        return maintenanceJobRepo.findByAssetIdOrderByScheduledDateDesc(assetId);
    }

    @Transactional
    public AssetBreakdown reportBreakdown(Long assetId, String symptom, Long assignedEmployeeId, AppUser reportedBy) {
        Asset asset = getAsset(assetId);
        asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        AssetBreakdown breakdown = AssetBreakdown.builder()
            .tenantId(asset.getTenantId())
            .asset(asset)
            .reportedAt(LocalDateTime.now())
            .reportedBy(reportedBy)
            .assignedEmployee(assignedEmployeeId != null ? employeeRepo.findById(assignedEmployeeId).orElse(null) : null)
            .symptom(symptom)
            .status(BreakdownStatus.OPEN)
            .build();
        return breakdownRepo.save(breakdown);
    }

    @Transactional
    public AssetBreakdown closeBreakdown(Long breakdownId, String rootCause, String repairAction, BigDecimal repairCost) {
        AssetBreakdown breakdown = breakdownRepo.findById(breakdownId)
            .orElseThrow(() -> new IllegalArgumentException("Breakdown not found: " + breakdownId));
        LocalDateTime repairedAt = LocalDateTime.now();
        breakdown.setRepairedAt(repairedAt);
        breakdown.setDowntimeMinutes(Duration.between(breakdown.getReportedAt(), repairedAt).toMinutes());
        breakdown.setRootCause(rootCause);
        breakdown.setRepairAction(repairAction);
        breakdown.setRepairCost(value(repairCost));
        breakdown.setStatus(BreakdownStatus.CLOSED);
        breakdown.getAsset().setStatus(AssetStatus.ACTIVE);
        return breakdownRepo.save(breakdown);
    }

    @Transactional(readOnly = true)
    public List<AssetBreakdown> breakdownsForAsset(Long assetId) {
        return breakdownRepo.findByAssetIdOrderByReportedAtDesc(assetId);
    }

    public BigDecimal mtbfHours(Long assetId) {
        List<AssetBreakdown> closed = breakdownsForAsset(assetId).stream()
            .filter(b -> b.getRepairedAt() != null)
            .sorted(Comparator.comparing(AssetBreakdown::getRepairedAt))
            .toList();
        if (closed.size() < 2) return BigDecimal.ZERO;
        long totalHours = 0;
        for (int i = 1; i < closed.size(); i++) {
            totalHours += ChronoUnit.HOURS.between(closed.get(i - 1).getRepairedAt(), closed.get(i).getReportedAt());
        }
        return BigDecimal.valueOf(totalHours).divide(BigDecimal.valueOf(closed.size() - 1), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyticsSummary() {
        return Map.of(
            "totalAssets", assetRepo.count(),
            "activeAssets", assetRepo.countByStatus(AssetStatus.ACTIVE),
            "maintenanceAssets", assetRepo.countByStatus(AssetStatus.UNDER_MAINTENANCE),
            "openBreakdowns", breakdownRepo.findByStatusOrderByReportedAtDesc(BreakdownStatus.OPEN).size(),
            "plannedMaintenance", maintenanceJobRepo.findByStatusOrderByScheduledDateAsc(MaintenanceJobStatus.PLANNED).size()
        );
    }

    private AssetMaintenanceJob createMaintenanceJob(PreventiveMaintenancePlan plan, boolean createJobCard) {
        JobCard jobCard = null;
        if (createJobCard) {
            Optional<Project> project = currentAssignment(plan.getAsset().getId()).map(AssetAssignment::getProject);
            if (project.isPresent()) {
                jobCard = JobCard.builder()
                    .tenantId(plan.getTenantId())
                    .project(project.get())
                    .phase("Preventive Maintenance - " + plan.getAsset().getAssetCode())
                    .description(plan.getInstructions())
                    .assignedEngineer(plan.getAssignedEmployee())
                    .targetDate(plan.getNextDueDate())
                    .status(JobCardStatus.PLANNED)
                    .build();
                jobCard = jobCardRepo.save(jobCard);
            }
        }

        return AssetMaintenanceJob.builder()
            .tenantId(plan.getTenantId())
            .asset(plan.getAsset())
            .maintenancePlan(plan)
            .jobCard(jobCard)
            .scheduledDate(plan.getNextDueDate())
            .assignedEmployee(plan.getAssignedEmployee())
            .status(MaintenanceJobStatus.PLANNED)
            .notes(plan.getInstructions())
            .build();
    }

    private LocalDate nextDueDate(PreventiveMaintenancePlan plan) {
        return switch (plan.getFrequency()) {
            case DAILY -> plan.getNextDueDate().plusDays(1);
            case WEEKLY -> plan.getNextDueDate().plusWeeks(1);
            case MONTHLY -> plan.getNextDueDate().plusMonths(1);
            case QUARTERLY -> plan.getNextDueDate().plusMonths(3);
            case HALF_YEARLY -> plan.getNextDueDate().plusMonths(6);
            case YEARLY -> plan.getNextDueDate().plusYears(1);
            case CUSTOM -> plan.getNextDueDate().plusDays(plan.getCustomIntervalDays() != null ? plan.getCustomIntervalDays() : 30);
        };
    }

    private BigDecimal value(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
