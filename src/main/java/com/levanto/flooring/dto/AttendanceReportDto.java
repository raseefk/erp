package com.levanto.flooring.dto;

import com.levanto.flooring.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportDto {
    private LocalDate date;
    private Employee employee;
    private LocalTime clockInTime;
    private LocalTime clockOutTime;
    private String status;
    private String notes;
    private boolean manualCorrection;
}
