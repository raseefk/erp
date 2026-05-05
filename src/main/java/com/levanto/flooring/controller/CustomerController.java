package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.Customer;
import com.levanto.flooring.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin/customers") @RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="20") int size,
                       @RequestParam(required=false) String search,
                       Model m) {
        m.addAttribute("customersPage", service.getAll(page, size, search));
        m.addAttribute("search",        search);
        m.addAttribute("currentPage",   page);
        return "customer/list";
    }

    @GetMapping("/new")
    public String newForm(Model m) { m.addAttribute("customer", new Customer()); return "customer/form"; }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("customer", service.getById(id)); return "customer/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Customer c, RedirectAttributes ra) {
        service.save(c);
        ra.addFlashAttribute("success", "Customer saved.");
        return "redirect:/admin/customers";
    }

    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted."));
    }
}
