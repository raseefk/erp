package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/admin/scm/approvals")
@RequiredArgsConstructor
@RequiresFeature("SCM")
public class ProcurementApprovalController {

    private final ProcurementService procurementService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pendingApprovals", procurementService.getAllPendingApprovals());
        return "scm/procurement-approvals";
    }

    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> approve(@PathVariable Long id,
                                                   @RequestParam(required = false) String comments,
                                                   Principal principal) {
        try {
            String approverName = principal != null ? principal.getName() : "Unknown";
            procurementService.approveRequest(id, comments, approverName);
            return ResponseEntity.ok(ApiResponse.ok("Request approved. PO status updated."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> reject(@PathVariable Long id,
                                                  @RequestParam(required = false) String comments,
                                                  Principal principal) {
        try {
            String approverName = principal != null ? principal.getName() : "Unknown";
            procurementService.rejectRequest(id, comments, approverName);
            return ResponseEntity.ok(ApiResponse.ok("Request rejected. PO cancelled."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
