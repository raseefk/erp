package com.supererp.erp.rbac;

/**
 * Single source of truth for all system permissions.
 * These constants are synced to the 'permissions' table on startup
 * by PermissionSeeder.
 *
 * Naming convention: {FEATURE}_{MENU}_{ACTION}
 */
public final class Permissions {

    private Permissions() {}

    // ── GENERAL ──────────────────────────────────────────────────────────────
    public static final String DASHBOARD_VIEW               = "DASHBOARD_VIEW";

    // ── BILLING Feature ──────────────────────────────────────────────────────
    public static final String BILLING_INVOICES_VIEW        = "BILLING_INVOICES_VIEW";
    public static final String BILLING_INVOICES_CREATE      = "BILLING_INVOICES_CREATE";
    public static final String BILLING_INVOICES_EDIT        = "BILLING_INVOICES_EDIT";
    public static final String BILLING_INVOICES_DELETE      = "BILLING_INVOICES_DELETE";
    public static final String BILLING_INVOICES_APPROVE     = "BILLING_INVOICES_APPROVE";
    public static final String BILLING_INVOICES_EXPORT_PDF  = "BILLING_INVOICES_EXPORT_PDF";
    public static final String BILLING_INVOICES_CONVERT     = "BILLING_INVOICES_CONVERT";
    public static final String BILLING_PAYMENTS_VIEW        = "BILLING_PAYMENTS_VIEW";
    public static final String BILLING_PAYMENTS_RECORD      = "BILLING_PAYMENTS_RECORD";

    // ── CRM Feature ──────────────────────────────────────────────────────────
    public static final String CRM_CUSTOMERS_VIEW           = "CRM_CUSTOMERS_VIEW";
    public static final String CRM_CUSTOMERS_CREATE         = "CRM_CUSTOMERS_CREATE";
    public static final String CRM_CUSTOMERS_EDIT           = "CRM_CUSTOMERS_EDIT";
    public static final String CRM_CUSTOMERS_DELETE         = "CRM_CUSTOMERS_DELETE";
    public static final String CRM_ENQUIRIES_VIEW           = "CRM_ENQUIRIES_VIEW";
    public static final String CRM_ENQUIRIES_MANAGE         = "CRM_ENQUIRIES_MANAGE";

    // ── INVENTORY Feature ─────────────────────────────────────────────────────
    public static final String INV_ITEMS_VIEW               = "INV_ITEMS_VIEW";
    public static final String INV_ITEMS_CREATE             = "INV_ITEMS_CREATE";
    public static final String INV_ITEMS_EDIT               = "INV_ITEMS_EDIT";
    public static final String INV_ITEMS_DELETE             = "INV_ITEMS_DELETE";
    public static final String INV_ITEMS_ADJUST_STOCK       = "INV_ITEMS_ADJUST_STOCK";

    // ── SCM Feature ───────────────────────────────────────────────────────────
    public static final String SCM_PO_VIEW                  = "SCM_PO_VIEW";
    public static final String SCM_PO_CREATE                = "SCM_PO_CREATE";
    public static final String SCM_PO_EDIT                  = "SCM_PO_EDIT";
    public static final String SCM_PO_APPROVE               = "SCM_PO_APPROVE";
    public static final String SCM_PO_RECEIVE               = "SCM_PO_RECEIVE";
    public static final String SCM_VENDORS_VIEW             = "SCM_VENDORS_VIEW";
    public static final String SCM_VENDORS_CREATE           = "SCM_VENDORS_CREATE";
    public static final String SCM_VENDORS_EDIT             = "SCM_VENDORS_EDIT";

    // ── PROJECTS Feature ──────────────────────────────────────────────────────
    public static final String PROJ_LIST_VIEW               = "PROJ_LIST_VIEW";
    public static final String PROJ_LIST_CREATE             = "PROJ_LIST_CREATE";
    public static final String PROJ_LIST_EDIT               = "PROJ_LIST_EDIT";
    public static final String PROJ_LIST_DELETE             = "PROJ_LIST_DELETE";
    public static final String PROJ_JOBCARD_VIEW            = "PROJ_JOBCARD_VIEW";
    public static final String PROJ_JOBCARD_MANAGE          = "PROJ_JOBCARD_MANAGE";
    public static final String PROJ_DAILYLOG_VIEW           = "PROJ_DAILYLOG_VIEW";
    public static final String PROJ_DAILYLOG_ADD            = "PROJ_DAILYLOG_ADD";
    public static final String PROJ_EXPENSES_VIEW           = "PROJ_EXPENSES_VIEW";
    public static final String PROJ_EXPENSES_ADD            = "PROJ_EXPENSES_ADD";
    public static final String PROJ_EXPENSES_APPROVE        = "PROJ_EXPENSES_APPROVE";

    // ── HR Feature ────────────────────────────────────────────────────────────
    public static final String HR_EMPLOYEES_VIEW            = "HR_EMPLOYEES_VIEW";
    public static final String HR_EMPLOYEES_CREATE          = "HR_EMPLOYEES_CREATE";
    public static final String HR_EMPLOYEES_EDIT            = "HR_EMPLOYEES_EDIT";
    public static final String HR_EMPLOYEES_DELETE          = "HR_EMPLOYEES_DELETE";
    public static final String HR_ATTENDANCE_VIEW           = "HR_ATTENDANCE_VIEW";
    public static final String HR_ATTENDANCE_MARK           = "HR_ATTENDANCE_MARK";
    public static final String HR_LEAVES_VIEW               = "HR_LEAVES_VIEW";
    public static final String HR_LEAVES_APPLY              = "HR_LEAVES_APPLY";
    public static final String HR_LEAVES_APPROVE            = "HR_LEAVES_APPROVE";
    public static final String HR_SALARY_VIEW               = "HR_SALARY_VIEW";
    public static final String HR_SALARY_PAY               = "HR_SALARY_PAY";
    public static final String HR_HOLIDAYS_MANAGE           = "HR_HOLIDAYS_MANAGE";
    public static final String HR_ATTENDANCE_REPORT_VIEW   = "HR_ATTENDANCE_REPORT_VIEW";

    // ── FINANCE Feature ───────────────────────────────────────────────────────
    public static final String FIN_TRANSACTIONS_VIEW        = "FIN_TRANSACTIONS_VIEW";
    public static final String FIN_EXPENSES_VIEW            = "FIN_EXPENSES_VIEW";
    public static final String FIN_EXPENSES_CREATE          = "FIN_EXPENSES_CREATE";
    public static final String FIN_EXPENSES_APPROVE         = "FIN_EXPENSES_APPROVE";
    public static final String FIN_PAYMENTS_VIEW            = "FIN_PAYMENTS_VIEW";
    public static final String FIN_PAYMENTS_RECORD          = "FIN_PAYMENTS_RECORD";
    public static final String FIN_LEDGER_VIEW              = "FIN_LEDGER_VIEW";
    public static final String FIN_REPORTS_VIEW             = "FIN_REPORTS_VIEW";

    // ── SETTINGS Feature (Admin) ───────────────────────────────────────────────
    public static final String SETTINGS_COMPANY_VIEW        = "SETTINGS_COMPANY_VIEW";
    public static final String SETTINGS_COMPANY_EDIT        = "SETTINGS_COMPANY_EDIT";
    public static final String SETTINGS_USERS_VIEW          = "SETTINGS_USERS_VIEW";
    public static final String SETTINGS_USERS_CREATE        = "SETTINGS_USERS_CREATE";
    public static final String SETTINGS_USERS_EDIT          = "SETTINGS_USERS_EDIT";
    public static final String SETTINGS_USERS_DELETE        = "SETTINGS_USERS_DELETE";
    public static final String SETTINGS_ROLES_VIEW          = "SETTINGS_ROLES_VIEW";
    public static final String SETTINGS_ROLES_MANAGE        = "SETTINGS_ROLES_MANAGE";
    public static final String SETTINGS_FEATURES_MANAGE     = "SETTINGS_FEATURES_MANAGE";
}
