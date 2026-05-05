package com.levanto.flooring.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EmployeeSummary {
    Long getId();
    String getEmployeeCode();
    String getName();
    String getEmail();
    String getPhone();
    String getDesignation();
    BigDecimal getMonthlySalary();
    LocalDate getJoiningDate();
}
