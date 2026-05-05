package com.supererp.erp.projection;

public interface VendorSummary {
    Long getId();
    String getName();
    String getEmail();
    String getPhone();
    String getContactPerson();
    String getMaterialSupplied();
    String getGstNumber();
    String getBankName();
    String getBankAccountNumber();
    String getIfscCode();
    boolean isActive();
}
