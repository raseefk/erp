package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.VendorRating;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.service.ProcurementService;
import com.supererp.erp.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/scm/vendor-ratings")
@RequiredArgsConstructor
@RequiresFeature("SCM")
public class VendorRatingController {

    private final ProcurementService procurementService;
    private final VendorService vendorService;

    @GetMapping
    public String scorecard(Model model) {
        model.addAttribute("ratings", procurementService.getAllRatings(0, 50, null));
        model.addAttribute("scorecardSummary", procurementService.getVendorScorecardSummary());
        return "scm/vendor-scorecard";
    }

    @GetMapping("/{vendorId}")
    public String vendorRatings(@PathVariable Long vendorId, Model model) {
        model.addAttribute("vendor", vendorService.getById(vendorId));
        model.addAttribute("ratings", procurementService.getRatingsForVendor(vendorId));
        model.addAttribute("averageScore", procurementService.getVendorAverageScore(vendorId));
        return "scm/vendor-ratings";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute VendorRating rating, Principal principal, RedirectAttributes ra) {
        try {
            rating.setRatedBy(principal != null ? principal.getName() : "System");
            procurementService.saveVendorRating(rating);
            ra.addFlashAttribute("success", "Vendor rating saved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/vendor-ratings";
    }
}
