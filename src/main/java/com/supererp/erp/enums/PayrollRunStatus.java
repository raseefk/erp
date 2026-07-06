package com.supererp.erp.enums;

public enum PayrollRunStatus {
    DRAFT,      // Being computed, not yet approved
    APPROVED,   // Approved by HR/Finance manager
    DISBURSED,  // Bank file generated / payments made
    CANCELLED   // Voided
}
