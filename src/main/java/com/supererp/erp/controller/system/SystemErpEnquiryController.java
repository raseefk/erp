package com.supererp.erp.controller.system;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.ErpEnquiry;
import com.supererp.erp.enums.EnquiryStatus;
import com.supererp.erp.service.ErpEnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ERP (product/service) enquiry management for admin users.
 * Previously restricted to SYSTEM_ADMIN; now accessible to ROLE_ADMIN users.
 */
@Controller
@RequestMapping("/admin/erp-enquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SystemErpEnquiryController {

    private final ErpEnquiryService service;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       Model model) {
        EnquiryStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = EnquiryStatus.valueOf(status.toUpperCase()); } catch (Exception ignored) {}
        }
        Page<ErpEnquiry> enquiriesPage = service.getAll(page, size, search, st);
        model.addAttribute("enquiriesPage",  enquiriesPage);
        model.addAttribute("search",         search);
        model.addAttribute("statusFilter",   status);
        model.addAttribute("currentPage",    page);
        model.addAttribute("EnquiryStatus",  EnquiryStatus.values());
        model.addAttribute("newCount",       service.countNew());
        model.addAttribute("contactedCount", service.countContacted());
        model.addAttribute("pageTitle",      "ERP Enquiries");
        return "system/erp-enquiry/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("enquiry",       service.getById(id));
        model.addAttribute("EnquiryStatus", EnquiryStatus.values());
        model.addAttribute("pageTitle",     "View ERP Enquiry");
        return "system/erp-enquiry/view";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes ra) {
        try {
            service.updateStatus(id, EnquiryStatus.valueOf(status), notes);
            ra.addFlashAttribute("successMessage", "Enquiry status updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/erp-enquiries/" + id;
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Enquiry deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
