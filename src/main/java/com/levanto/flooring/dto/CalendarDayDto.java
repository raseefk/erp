package com.levanto.flooring.dto;

import com.levanto.flooring.entity.Attendance;
import com.levanto.flooring.entity.LeaveApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarDayDto {
    private LocalDate date;
    private Attendance attendance;
    private LeaveApplication approvedLeave;
    private boolean isLeave;
    private boolean isWeekend;
    private boolean isFuture;
    private boolean isHoliday;
    private String holidayName;
}
