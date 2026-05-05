package com.levanto.flooring.service;

import com.levanto.flooring.dto.EnquiryRequest;
import com.levanto.flooring.entity.Enquiry;
import com.levanto.flooring.enums.EnquiryStatus;
import com.levanto.flooring.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository repo;

    @Transactional
    public Enquiry submit(EnquiryRequest req) {
        return repo.save(Enquiry.builder()
            .name(req.getName()).phone(req.getPhone()).email(req.getEmail())
            .service(req.getService()).message(req.getMessage())
            .status(EnquiryStatus.NEW).build());
    }

    public Page<com.levanto.flooring.projection.EnquirySummary> getAll(int page, int size, String q, EnquiryStatus status) {
        Pageable pg = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return repo.searchSummaries(q != null ? q.trim() : null, status, pg);
    }

    public Enquiry getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Enquiry not found: " + id));
    }

    @Transactional
    public Enquiry updateStatus(Long id, EnquiryStatus status, String notes) {
        Enquiry e = getById(id);
        e.setStatus(status);
        if (notes != null && !notes.isBlank()) e.setAdminNotes(notes);
        return repo.save(e);
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    public long countNew()       { return repo.countByStatus(EnquiryStatus.NEW); }
    public long countContacted() { return repo.countByStatus(EnquiryStatus.CONTACTED); }
}
