package com.levanto.flooring.service;

import com.levanto.flooring.entity.Vendor;
import com.levanto.flooring.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class VendorService {

    private final VendorRepository repo;

    public Page<com.levanto.flooring.projection.VendorSummary> getAll(int page, int size, String q) {
        Pageable pg = PageRequest.of(page, size, Sort.by("name").ascending());
        return repo.searchSummaries(q != null ? q.trim() : null, pg);
    }

    public List<Vendor> getActive() {
        return repo.findAllByActiveTrueOrderByNameAsc();
    }

    public Vendor getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + id));
    }

    @Transactional public Vendor save(Vendor v)     { return repo.save(v); }
    @Transactional public void   delete(Long id)    { repo.deleteById(id); }

    @Transactional
    public void toggleActive(Long id) {
        Vendor v = getById(id); v.setActive(!v.isActive()); repo.save(v);
    }
}
