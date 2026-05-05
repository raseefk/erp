package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.Vendor;
import com.levanto.flooring.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin/vendors") @RequiredArgsConstructor
public class VendorController {

    private final VendorService service;

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="20") int size,
                       @RequestParam(required=false) String search,
                       Model m) {
        m.addAttribute("vendorsPage", service.getAll(page, size, search));
        m.addAttribute("search",      search);
        m.addAttribute("currentPage", page);
        return "vendor/list";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("vendor", new Vendor());
        return "vendor/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("vendor", service.getById(id));
        return "vendor/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Vendor vendor, RedirectAttributes ra) {
        service.save(vendor);
        ra.addFlashAttribute("success", "Vendor saved.");
        return "redirect:/admin/vendors";
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
