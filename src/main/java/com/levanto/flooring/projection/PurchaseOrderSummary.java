package com.levanto.flooring.projection;

import com.levanto.flooring.enums.PurchaseOrderStatus;
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
