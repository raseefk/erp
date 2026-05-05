package com.levanto.flooring.projection;

import com.levanto.flooring.enums.EnquiryStatus;
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
