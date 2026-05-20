package com.supererp.erp.service;

import com.supererp.erp.dto.ErpEnquiryRequest;
import com.supererp.erp.entity.ErpEnquiry;
import com.supererp.erp.enums.EnquiryStatus;
import com.supererp.erp.repository.ErpEnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ErpEnquiryService {

    private final ErpEnquiryRepository repo;

    @Transactional
    public ErpEnquiry submit(ErpEnquiryRequest req) {
        return repo.save(ErpEnquiry.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .company(req.getCompany())
                .industry(req.getIndustry())
                .plan(req.getPlan())
                .message(req.getMessage())
                .status(EnquiryStatus.NEW)
                .build());
    }

    public Page<ErpEnquiry> getAll(int page, int size, String q, EnquiryStatus status) {
        Pageable pg = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return repo.search(q != null ? q.trim() : null, status, pg);
    }

    public ErpEnquiry getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ERP Enquiry not found: " + id));
    }

    @Transactional
    public ErpEnquiry updateStatus(Long id, EnquiryStatus status, String notes) {
        ErpEnquiry e = getById(id);
        e.setStatus(status);
        if (notes != null && !notes.isBlank()) {
            e.setAdminNotes(notes);
        }
        return repo.save(e);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    public long countNew() {
        return repo.countByStatus(EnquiryStatus.NEW);
    }

    public long countContacted() {
        return repo.countByStatus(EnquiryStatus.CONTACTED);
    }
}
