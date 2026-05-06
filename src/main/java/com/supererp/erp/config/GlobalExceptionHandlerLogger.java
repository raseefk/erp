package com.supererp.erp.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandlerLogger {

    @ExceptionHandler(com.supererp.erp.rbac.exception.FeatureDisabledException.class)
    public Object handleFeatureDisabled(com.supererp.erp.rbac.exception.FeatureDisabledException ex, WebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        boolean isHtmlRequest = acceptHeader != null && acceptHeader.contains("text/html");

        if (isHtmlRequest) {
            org.springframework.web.servlet.ModelAndView mav = new org.springframework.web.servlet.ModelAndView("error/feature-blocked");
            mav.addObject("featureName", ex.getFeatureName());
            return mav;
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(new com.supererp.erp.dto.ApiResponse<>(false, ex.getMessage(), null));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        try (FileWriter fw = new FileWriter("error_log.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("----- " + LocalDateTime.now() + " -----");
            pw.println("Request: " + request.getDescription(false));
            ex.printStackTrace(pw);
            pw.println("---------------------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Return 500 so the frontend fetch() sees it
        return ResponseEntity.internalServerError().body(new com.supererp.erp.dto.ApiResponse<>(false, ex.getMessage(), null));
    }
}
