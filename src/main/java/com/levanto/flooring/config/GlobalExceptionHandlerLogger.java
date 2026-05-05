package com.levanto.flooring.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandlerLogger {

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
        return ResponseEntity.internalServerError().body(new com.levanto.flooring.dto.ApiResponse<>(false, ex.getMessage(), null));
    }
}
