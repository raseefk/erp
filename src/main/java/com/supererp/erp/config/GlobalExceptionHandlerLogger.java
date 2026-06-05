package com.supererp.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
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

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        boolean isHtmlRequest = acceptHeader != null && acceptHeader.contains("text/html");

        if (isHtmlRequest) {
            org.springframework.web.servlet.ModelAndView mav = new org.springframework.web.servlet.ModelAndView("error/access-denied");
            String msg = ex.getMessage();
            if (msg != null && msg.contains(": ")) {
                mav.addObject("requiredPermission", msg.substring(msg.indexOf(": ") + 2));
            }
            return mav;
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(new com.supererp.erp.dto.ApiResponse<>(false, ex.getMessage(), null));
        }
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest request) {
        log.warn("Resource not found: {} - {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
            .body(new com.supererp.erp.dto.ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        // Log via SLF4J — output destination is controlled by logging.file.name in application properties.
        // In production (Docker), this writes to /app/logs/super-erp.log on the VM volume.
        log.error("Unhandled exception on request [{}]: {}", request.getDescription(false), ex.getMessage(), ex);

        return ResponseEntity.internalServerError()
            .body(new com.supererp.erp.dto.ApiResponse<>(false, ex.getMessage(), null));
    }
}
