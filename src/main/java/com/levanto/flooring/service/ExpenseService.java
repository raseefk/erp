package com.levanto.flooring.service;

import com.levanto.flooring.entity.Expense;
import com.levanto.flooring.enums.ExpenseCategory;
import com.levanto.flooring.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class ExpenseService {

    private final ExpenseRepository  repo;
    private final FileStorageService fileStorage;

    // ── Paged (for list screen) ───────────────────────────────────────────────
    public Page<Expense> getAll(int page, int size,
                                LocalDate from, LocalDate to,
                                ExpenseCategory category) {
        Pageable pg = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = to   != null ? to   : LocalDate.now();

        if (category != null)
            return repo.findByCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(category, f, t, pg);
        return repo.findByExpenseDateBetweenOrderByExpenseDateDesc(f, t, pg);
    }

    // ── All records matching filter — used by PDF export ─────────────────────
    public List<Expense> getAllForExport(LocalDate from, LocalDate to, ExpenseCategory category) {
        LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = to   != null ? to   : LocalDate.now();

        if (category != null)
            return repo.findByCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(category, f, t);
        // For full export: order by category then date so grouping works naturally
        return repo.findByExpenseDateBetweenOrderByCategoryAscExpenseDateDesc(f, t);
    }

    public Expense getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
    }

    @Transactional
    public Expense save(Expense expense, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                String path = fileStorage.store(file, "expenses");
                expense.setAttachmentPath(path);
                expense.setAttachmentName(file.getOriginalFilename());
                expense.setAttachmentMimeType(file.getContentType());
            } catch (IOException e) {
                throw new RuntimeException("File upload failed: " + e.getMessage(), e);
            }
        }
        return repo.save(expense);
    }

    @Transactional
    public void delete(Long id) {
        Expense e = getById(id);
        if (e.getAttachmentPath() != null) fileStorage.delete(e.getAttachmentPath());
        repo.delete(e);
    }

    public BigDecimal totalForPeriod(LocalDate from, LocalDate to) {
        return repo.sumByDateRange(from, to);
    }

    public BigDecimal totalForCategory(ExpenseCategory cat, LocalDate from, LocalDate to) {
        return repo.sumByCategoryAndDateRange(cat, from, to);
    }
}
