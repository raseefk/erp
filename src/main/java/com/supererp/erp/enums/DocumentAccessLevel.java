package com.supererp.erp.enums;

public enum DocumentAccessLevel {
    PUBLIC,       // All authenticated users in tenant
    DEPARTMENT,   // Specific department
    ROLE,         // Specific role only
    PRIVATE       // Owner only
}
