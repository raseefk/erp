package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.Employee;
import com.supererp.erp.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller @RequestMapping("/admin/employees") @RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService service;
    private final com.supererp.erp.service.SalaryService salaryService;
    private final com.supererp.erp.repository.AppUserRepository userRepo;
    private final com.supererp.erp.rbac.service.RbacService rbacService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model m, 
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search) {
        org.springframework.data.domain.Page<com.supererp.erp.projection.EmployeeSummary> pg = service.getAllPaginated(page, size, search);
        m.addAttribute("employeesPage", pg);
        m.addAttribute("employees", pg.getContent());
        m.addAttribute("currentPage", page);
        m.addAttribute("search", search);
        return "employee/list";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("employee", new Employee());
        m.addAttribute("users", userRepo.findAllByEnabledTrueOrderByFullNameAsc());
        m.addAttribute("roles", rbacService.getRoles(com.supererp.erp.tenant.TenantContext.getTenantId()));
        return "employee/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("employee", service.getById(id));
        m.addAttribute("users", userRepo.findAllByEnabledTrueOrderByFullNameAsc());
        m.addAttribute("roles", rbacService.getRoles(com.supererp.erp.tenant.TenantContext.getTenantId()));
        return "employee/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Employee emp, 
                       @RequestParam(required = false) Long userId,
                       @RequestParam(required = false) Boolean createUser,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String password,
                       @RequestParam(required = false) Long roleId,
                       RedirectAttributes ra) {
        
        if (Boolean.TRUE.equals(createUser) && username != null && !username.isBlank()) {
            if (userRepo.existsByUsername(username)) {
                ra.addFlashAttribute("error", "Username already exists.");
                return "redirect:/admin/employees/new";
            }
            
            java.util.Set<com.supererp.erp.rbac.entity.AppRole> roles = new java.util.HashSet<>();
            if (roleId != null) {
                rbacService.getRole(roleId).ifPresent(roles::add);
            }

            com.supererp.erp.entity.AppUser newUser = com.supererp.erp.entity.AppUser.builder()
                .tenantId(com.supererp.erp.tenant.TenantContext.getTenantId())
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(emp.getName())
                .roles(roles)
                .enabled(true)
                .build();
            newUser = userRepo.save(newUser);
            emp.setAppUser(newUser);
        } else if (userId != null) {
            emp.setAppUser(userRepo.findById(userId).orElse(null));
        }
        
        service.save(emp);
        ra.addFlashAttribute("success", "Employee saved.");
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/pay") @ResponseBody
    public ResponseEntity<ApiResponse<?>> paySalary(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate payDate) {
        salaryService.paySalary(id, amount, payDate);
        return ResponseEntity.ok(ApiResponse.ok("Salary recorded and expense entry created."));
    }

    @PostMapping("/{id}/toggle") @ResponseBody
    public ResponseEntity<ApiResponse<?>> toggle(@PathVariable Long id) {
        service.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok("Status toggled."));
    }

    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted."));
    }
}
