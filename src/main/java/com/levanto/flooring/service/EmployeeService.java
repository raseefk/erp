package com.levanto.flooring.service;

import com.levanto.flooring.entity.Employee;
import com.levanto.flooring.entity.Expense;
import com.levanto.flooring.enums.ExpenseCategory;
import com.levanto.flooring.repository.EmployeeRepository;
import com.levanto.flooring.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service @RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository empRepo;
    private final ExpenseRepository  expRepo;

    public List<Employee> getAll()  { return empRepo.findByActiveTrueOrderByNameAsc(); }

    public org.springframework.data.domain.Page<com.levanto.flooring.projection.EmployeeSummary> getAllPaginated(int page, int size, String search) {
        org.springframework.data.domain.Pageable pg = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("name").ascending());
        if (search != null && !search.isBlank()) {
            return empRepo.searchSummary(search, pg);
        }
        return empRepo.findByActiveTrueOrderByNameAsc(pg);
    }

    public List<Employee> getActive() { return empRepo.findByActiveTrueOrderByNameAsc(); }

    public Employee getById(Long id) {
        return empRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    @Transactional
    public Employee save(Employee emp) {
        if (emp.getId() == null && (emp.getEmployeeCode() == null || emp.getEmployeeCode().isBlank())) {
            emp.setEmployeeCode(generateNextCode());
        }
        return empRepo.save(emp);
    }

    private String generateNextCode() {
        return empRepo.findTopByOrderByIdDesc()
            .map(e -> {
                Long lastId = e.getId();
                return "EMP" + String.format("%03d", lastId + 1);
            })
            .orElse("EMP001");
    }

    @Transactional
    public void toggleActive(Long id) {
        Employee e = getById(id);
        e.setActive(!e.isActive());
        empRepo.save(e);
    }

    @Transactional
    public void delete(Long id) { empRepo.deleteById(id); }


}
