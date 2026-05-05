package com.supererp.erp.projection;

import com.supererp.erp.enums.EnquiryStatus;
import java.time.LocalDateTime;

public interface EnquirySummary {
    Long getId();
    String getName();
    String getEmail();
    String getPhone();
    String getService();
    EnquiryStatus getStatus();
    LocalDateTime getSubmittedAt();
}
