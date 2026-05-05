package com.supererp.erp.projection;

import com.supererp.erp.enums.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PurchaseOrderSummary {
    Long getId();
    String getPoNumber();
    String getVendorName();
    LocalDate getOrderDate();
    LocalDate getExpectedDeliveryDate();
    BigDecimal getTotalAmount();
    PurchaseOrderStatus getStatus();
}
