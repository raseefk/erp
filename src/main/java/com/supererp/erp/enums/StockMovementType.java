package com.supererp.erp.enums;

public enum StockMovementType {
    RECEIPT,          // Goods received (GRN)
    ISSUE,            // Stock issued / consumed
    TRANSFER_OUT,     // Transferred to another location
    TRANSFER_IN,      // Received from another location
    ADJUSTMENT_IN,    // Manual positive adjustment
    ADJUSTMENT_OUT,   // Manual negative adjustment
    RETURN,           // Customer / vendor return
    OPENING,          // Opening stock entry
    STOCKTAKE         // Physical count adjustment
}
