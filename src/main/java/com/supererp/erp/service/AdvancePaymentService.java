package com.supererp.erp.service;

import com.supererp.erp.entity.AdvancePayment;
import com.supererp.erp.entity.IncomeTransaction;
import com.supererp.erp.entity.Project;
import com.supererp.erp.enums.AdvancePaymentStatus;
import com.supererp.erp.repository.AdvancePaymentRepository;
import com.supererp.erp.repository.IncomeTransactionRepository;
import com.supererp.erp.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvancePaymentService {

    private final AdvancePaymentRepository advanceRepo;
    private final IncomeTransactionRepository incomeRepo;
    private final ProjectRepository projectRepo;

    public Page<AdvancePayment> getAdvances(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("date").descending());
        return advanceRepo.findAllByOrderByDateDesc(pg);
    }

    public List<AdvancePayment> getUnsettledAdvances(Long projectId) {
        if (projectId != null) {
            return advanceRepo.findByProject_IdAndStatusOrderByDateDesc(projectId, AdvancePaymentStatus.RECEIVED);
        }
        return advanceRepo.findByStatusOrderByDateDesc(AdvancePaymentStatus.RECEIVED);
    }

    public List<AdvancePayment> getAdvancesForProject(Long projectId) {
        return advanceRepo.findByProject_IdOrderByDateDesc(projectId);
    }

    public AdvancePayment getAdvanceById(Long id) {
        return advanceRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Advance not found"));
    }

    @Transactional
    public AdvancePayment createAdvance(String paymentFrom, Long projectId, BigDecimal amount, String description, LocalDate date) {
        Integer maxNum = advanceRepo.findMaxAdvanceNumber().orElse(0);
        String advanceNumber = String.format("ADV-%04d", maxNum + 1);

        Project project = null;
        if (projectId != null) {
            project = projectRepo.findById(projectId).orElse(null);
        }

        AdvancePayment advance = AdvancePayment.builder()
                .advanceNumber(advanceNumber)
                .paymentFrom(paymentFrom)
                .project(project)
                .amount(amount)
                .status(AdvancePaymentStatus.RECEIVED)
                .description(description)
                .date(date != null ? date : LocalDate.now())
                .build();

        advance = advanceRepo.save(advance);

        // Record in income ledger
        String finalDesc = (description != null && !description.isBlank()) ? description : "Advance Payment Received from " + paymentFrom;
        if (project != null) {
            finalDesc += " | Project: " + project.getName();
        }

        IncomeTransaction income = IncomeTransaction.builder()
                .transaction(null)
                .advancePayment(advance)
                .inventoryNumber(advanceNumber)
                .title("Advance Received")
                .amount(amount)
                .description(finalDesc)
                .date(advance.getDate())
                .build();

        incomeRepo.save(income);

        return advance;
    }
}
