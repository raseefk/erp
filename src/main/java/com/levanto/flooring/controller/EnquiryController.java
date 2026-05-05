package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.enums.EnquiryStatus;
import com.levanto.flooring.service.EnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin/enquiries") @RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService service;

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="20") int size,
                       @RequestParam(required=false) String search,
                       @RequestParam(required=false) String status,
                       Model m) {
        EnquiryStatus st = parseStatus(status);
        m.addAttribute("enquiriesPage", service.getAll(page, size, search, st));
        m.addAttribute("search",        search);
        m.addAttribute("statusFilter",  status);
        m.addAttribute("currentPage",   page);
        m.addAttribute("EnquiryStatus", EnquiryStatus.values());
        m.addAttribute("newCount",      service.countNew());
        m.addAttribute("contactedCount",service.countContacted());
        return "enquiry/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model m) {
        m.addAttribute("enquiry",       service.getById(id));
        m.addAttribute("EnquiryStatus", EnquiryStatus.values());
        return "enquiry/view";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required=false) String notes,
                               RedirectAttributes ra) {
        service.updateStatus(id, EnquiryStatus.valueOf(status), notes);
        ra.addFlashAttribute("success", "Enquiry updated.");
        return "redirect:/admin/enquiries";
    }

    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted."));
    }

    private EnquiryStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return EnquiryStatus.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }
}
