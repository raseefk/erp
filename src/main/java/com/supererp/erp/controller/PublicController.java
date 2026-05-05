package com.supererp.erp.controller;
import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.dto.EnquiryRequest;
import com.supererp.erp.service.EnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller @RequiredArgsConstructor
public class PublicController {
    private final EnquiryService enquiryService;

    @GetMapping("/") public String index() { return "index"; }

    @PostMapping("/api/enquiries/submit") @ResponseBody
    public ResponseEntity<ApiResponse<?>> submit(@Valid @RequestBody EnquiryRequest req) {
        enquiryService.submit(req);
        return ResponseEntity.ok(ApiResponse.ok("Thank you! We will contact you shortly."));
    }
}
