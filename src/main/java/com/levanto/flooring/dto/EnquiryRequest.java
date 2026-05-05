package com.levanto.flooring.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EnquiryRequest {
    @NotBlank @Size(max = 150) private String name;
    @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit mobile") private String phone;
    @Email @Size(max = 200)   private String email;
    @Size(max = 100)           private String service;
    @Size(max = 2000)          private String message;
}
