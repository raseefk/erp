package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.dto.ErpEnquiryRequest;
import com.supererp.erp.service.ErpEnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/erp-enquiries")
@RequiredArgsConstructor
public class PublicErpEnquiryController {

    private final ErpEnquiryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> submit(@Valid @RequestBody ErpEnquiryRequest req) {
        service.submit(req);
        return ResponseEntity.ok(ApiResponse.ok("Thank you! Your enquiry has been submitted. We'll get back to you within 24 hours."));
    }
}
