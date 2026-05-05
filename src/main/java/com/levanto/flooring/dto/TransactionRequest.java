package com.levanto.flooring.dto;

import com.levanto.flooring.enums.ItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TransactionRequest {

    @NotNull(message = "Customer is required")
    private Long customerId;

    private boolean gstEnabled  = true;
    private boolean taxAllItems = false;
    private String  gstType     = "LOCAL"; // LOCAL, IGST

    private String notes;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<LineItemRequest> items;

    @Data
    public static class LineItemRequest {

        @NotNull
        private ItemType itemType = ItemType.PRODUCT;

        private Long   inventoryItemId; // null for ad-hoc / service
        private String description;     // required for SERVICE or ad-hoc
        private String hsnSacCode;

        /** Square feet (primary for flooring) */
        private BigDecimal squareFeet = BigDecimal.ZERO;

        /** Generic quantity fallback */
        @NotNull @DecimalMin("0.001")
        private BigDecimal quantity = BigDecimal.ONE;

        private String unit = "SQF";

        @NotNull @DecimalMin("0.00")
        private BigDecimal ratePerUnit;

        /** GST % for this line (0–28) */
        @DecimalMin("0") @DecimalMax("28")
        private BigDecimal gstPercent;
    }
}
