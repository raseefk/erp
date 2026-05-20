package com.supererp.erp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ErpEnquiryRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{8,15}$", message = "Enter a valid phone number (8-15 digits)")
    private String phone;

    @Size(max = 255, message = "Company must not exceed 255 characters")
    private String company;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 50, message = "Plan must not exceed 50 characters")
    private String plan;

    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;
}
