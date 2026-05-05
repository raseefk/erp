package com.supererp.erp.service;

import com.supererp.erp.entity.Customer;
import com.supererp.erp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repo;

    public Page<com.supererp.erp.projection.CustomerSummary> getAll(int page, int size, String q) {
        Pageable pg = PageRequest.of(page, size, Sort.by("name").ascending());
        return repo.searchSummaries(q != null ? q.trim() : null, pg);
    }

    public Customer getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    public List<Customer> quickSearch(String q) {
        return repo.findTop10ByNameContainingIgnoreCaseOrPhoneContaining(q, q);
    }

    @Transactional public Customer save(Customer c) { return repo.save(c); }
    @Transactional public void     delete(Long id)  { repo.deleteById(id); }
}
