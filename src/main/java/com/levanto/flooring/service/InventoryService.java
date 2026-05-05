package com.levanto.flooring.service;

import com.levanto.flooring.entity.InventoryItem;
import com.levanto.flooring.enums.ItemType;
import com.levanto.flooring.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository repo;

    public List<InventoryItem> getAll()                   { return repo.findByActiveTrueOrderByNameAsc(); }
    public List<InventoryItem> searchActive(String q)     { return repo.searchActive(q); }
    public List<InventoryItem> getLowStock(int threshold) {
        return repo.findByItemTypeAndActiveTrueAndStockQuantityLessThan(ItemType.PRODUCT, threshold);
    }

    public Page<InventoryItem> paged(int page, int size, String q) {
        Pageable pg = PageRequest.of(page, size, Sort.by("name").ascending());
        return q != null && !q.isBlank() ? repo.searchAll(q, pg) : repo.findAll(pg);
    }

    public InventoryItem getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    @Transactional
    public InventoryItem save(InventoryItem item) { return repo.save(item); }

    @Transactional
    public void addStock(Long id, int qty) {
        InventoryItem item = getById(id);
        if (item.getItemType() != ItemType.PRODUCT)
            throw new IllegalStateException("Cannot add stock to a SERVICE item.");
        item.setStockQuantity(item.getStockQuantity() + qty);
        repo.save(item);
    }

    @Transactional
    public InventoryItem updatePrice(Long id, BigDecimal price) {
        InventoryItem item = getById(id);
        item.setCurrentPrice(price);
        return repo.save(item);
    }

    @Transactional
    public void toggleActive(Long id) {
        InventoryItem item = getById(id);
        item.setActive(!item.isActive());
        repo.save(item);
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }
}
