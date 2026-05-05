package com.levanto.flooring.projection;

import com.levanto.flooring.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProjectSummary {
    Long getId();
    String getName();
    String getClientName();
    String getLocation();
    BigDecimal getTotalContractValue();
    LocalDate getStartDate();
    LocalDate getEndDate();
    ProjectStatus getStatus();
    Integer getJobCardCount();
}
