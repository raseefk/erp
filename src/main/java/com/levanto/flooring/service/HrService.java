package com.levanto.flooring.service;

import com.levanto.flooring.entity.*;
import com.levanto.flooring.repository.AttendanceRepository;
import com.levanto.flooring.repository.EmployeeRepository;
import com.levanto.flooring.repository.LeaveApplicationRepository;
import com.levanto.flooring.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HrService {

    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanySettingsService companySettingsService;
    private final com.levanto.flooring.repository.HolidayRepository holidayRepository;

    // --- Attendance Methods ---

    public List<Attendance> getAttendanceForDate(LocalDate date) {
        return attendanceRepository.findByDateOrderByEmployeeNameAsc(date);
    }

    public List<Attendance> getAttendanceForEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByDateDesc(employeeId);
    }

    public org.springframework.data.domain.Page<com.levanto.flooring.dto.AttendanceReportDto> getAttendanceReportPaginated(Integer year, Integer month, Long employeeId, int page, int size) {
        List<com.levanto.flooring.dto.AttendanceReportDto> all = getAttendanceReport(year, month, employeeId);
        int start = Math.min(page * size, all.size());
        int end = Math.min((page + 1) * size, all.size());
        return new org.springframework.data.domain.PageImpl<>(all.subList(start, end), org.springframework.data.domain.PageRequest.of(page, size), all.size());
    }

    public List<com.levanto.flooring.dto.AttendanceReportDto> getAttendanceReport(Integer year, Integer month, Long employeeId) {
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        if (end.isAfter(LocalDate.now())) {
            end = LocalDate.now();
        }

        List<Employee> targetEmployees;
        if (employeeId != null) {
            targetEmployees = employeeRepository.findById(employeeId).map(java.util.Collections::singletonList).orElse(java.util.Collections.emptyList());
        } else {
            targetEmployees = employeeRepository.findAll();
        }

        List<com.levanto.flooring.dto.AttendanceReportDto> report = new java.util.ArrayList<>();
        List<java.time.DayOfWeek> weeklyOffDays = companySettingsService.getSettings().getWeeklyOffDaysList();

        for (Employee emp : targetEmployees) {
            List<Attendance> attendances = attendanceRepository.findByDateBetweenAndEmployeeIdOptional(start, end, emp.getId());
            List<LeaveApplication> leaves = leaveApplicationRepository.findApprovedLeavesInPeriod(emp.getId(), start, end);
            List<com.levanto.flooring.entity.Holiday> holidays = holidayRepository.findByDateBetween(start, end);

            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                final LocalDate current = d;
                Attendance att = attendances.stream().filter(a -> a.getDate().equals(current)).findFirst().orElse(null);
                LeaveApplication leave = leaves.stream().filter(l -> !current.isBefore(l.getStartDate()) && !current.isAfter(l.getEndDate())).findFirst().orElse(null);
                com.levanto.flooring.entity.Holiday holiday = holidays.stream().filter(h -> h.getDate().equals(current)).findFirst().orElse(null);

                String status = "ABSENT";
                if (att != null && att.getStatus() != null) {
                    status = att.getStatus().name();
                } else if (leave != null) {
                    status = leave.getLeaveType().name().equals("LOSS_OF_PAY") ? "LOSS OF PAY" : "ON LEAVE";
                } else if (holiday != null) {
                    status = "HOLIDAY";
                } else if (weeklyOffDays.contains(current.getDayOfWeek())) {
                    status = "WEEKEND";
                }

                report.add(com.levanto.flooring.dto.AttendanceReportDto.builder()
                        .date(current)
                        .employee(emp)
                        .clockInTime(att != null ? att.getClockInTime() : null)
                        .clockOutTime(att != null ? att.getClockOutTime() : null)
                        .status(status)
                        .notes(att != null && att.getAdminNotes() != null ? att.getAdminNotes() : (holiday != null ? holiday.getName() : ""))
                        .manualCorrection(att != null && att.isManualCorrection())
                        .build());
            }
        }
        
        // Sort by Date then Employee Name
        report.sort(java.util.Comparator.comparing(com.levanto.flooring.dto.AttendanceReportDto::getDate)
                .thenComparing(dto -> dto.getEmployee().getName()));
                
        return report;
    }

    @Transactional
    public Attendance clockIn(Long employeeId, LocalDate targetDate) {
        LocalDate today = targetDate != null ? targetDate : LocalDate.now();
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));
        
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        if (existing.isPresent()) {
            return existing.get(); // Already clocked in
        }

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(today)
                .clockInTime(LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                .status(Attendance.AttendanceStatus.PRESENT)
                .manualCorrection(today.isBefore(LocalDate.now())) // mark as manual correction if backdated
                .build();
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance clockOut(Long employeeId, LocalDate targetDate) {
        LocalDate today = targetDate != null ? targetDate : LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No clock-in record found for " + today));
        
        attendance.setClockOutTime(LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        if (today.isBefore(LocalDate.now())) {
             attendance.setManualCorrection(true);
        }
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance manualCorrection(Long attendanceId, LocalTime clockIn, LocalTime clockOut, Attendance.AttendanceStatus status, String notes) {
        Attendance attendance = attendanceRepository.findById(attendanceId).orElseThrow();
        attendance.setClockInTime(clockIn);
        attendance.setClockOutTime(clockOut);
        attendance.setStatus(status);
        attendance.setAdminNotes(notes);
        attendance.setManualCorrection(true);
        return attendanceRepository.save(attendance);
    }

    // --- Leave Methods ---

    public org.springframework.data.domain.Page<LeaveApplication> getAllLeaveApplicationsPaginated(int page, int size) {
        return leaveApplicationRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("appliedAt").descending()));
    }

    public List<LeaveApplication> getAllLeaveApplications() {
        return leaveApplicationRepository.findAllByOrderByAppliedAtDesc();
    }

    public List<LeaveApplication> getLeaveApplicationsForEmployee(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
    }

    @Transactional
    public LeaveBalance getOrCreateLeaveBalance(Long employeeId, int year) {
        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);
        if (balanceOpt.isPresent()) {
            return balanceOpt.get();
        }

        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        CompanySettings settings = companySettingsService.getSettings();

        LeaveBalance newBalance = LeaveBalance.builder()
                .employee(employee)
                .year(year)
                .allocatedSickLeaves(settings.getDefaultSickLeavesPerYear())
                .usedSickLeaves(0)
                .allocatedCasualLeaves(settings.getDefaultCasualLeavesPerYear())
                .usedCasualLeaves(0)
                .build();

        return leaveBalanceRepository.save(newBalance);
    }

    @Transactional
    public LeaveApplication applyForLeave(Long employeeId, LeaveApplication application) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(application.getStartDate(), application.getEndDate()) + 1;
        
        if (application.getLeaveType() != LeaveApplication.LeaveType.LOSS_OF_PAY) {
            int year = application.getStartDate().getYear();
            LeaveBalance balance = getOrCreateLeaveBalance(employeeId, year);
            if (application.getLeaveType() == LeaveApplication.LeaveType.SICK_LEAVE) {
                if (balance.getRemainingSickLeaves() < days) {
                    throw new RuntimeException("Insufficient Sick Leave balance. Available: " + balance.getRemainingSickLeaves() + ". Please apply for Loss of Pay (LOP).");
                }
            } else if (application.getLeaveType() == LeaveApplication.LeaveType.CASUAL_LEAVE) {
                if (balance.getRemainingCasualLeaves() < days) {
                    throw new RuntimeException("Insufficient Casual Leave balance. Available: " + balance.getRemainingCasualLeaves() + ". Please apply for Loss of Pay (LOP).");
                }
            }
        }

        application.setEmployee(employee);
        application.setStatus(LeaveApplication.LeaveStatus.PENDING);
        return leaveApplicationRepository.save(application);
    }

    @Transactional
    public LeaveApplication updateLeaveStatus(Long leaveId, LeaveApplication.LeaveStatus status) {
        LeaveApplication leave = leaveApplicationRepository.findById(leaveId).orElseThrow();
        
        // If transitioning to APPROVED, we might want to update balance
        if (leave.getStatus() != LeaveApplication.LeaveStatus.APPROVED && status == LeaveApplication.LeaveStatus.APPROVED) {
            int year = leave.getStartDate().getYear();
            LeaveBalance balance = getOrCreateLeaveBalance(leave.getEmployee().getId(), year);
            long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            
            if (leave.getLeaveType() == LeaveApplication.LeaveType.SICK_LEAVE) {
                balance.setUsedSickLeaves(balance.getUsedSickLeaves() + (int)days);
            } else if (leave.getLeaveType() == LeaveApplication.LeaveType.CASUAL_LEAVE) {
                balance.setUsedCasualLeaves(balance.getUsedCasualLeaves() + (int)days);
            }
            leaveBalanceRepository.save(balance);
        } else if (leave.getStatus() == LeaveApplication.LeaveStatus.APPROVED && status != LeaveApplication.LeaveStatus.APPROVED) {
            // Reverting approval, subtract from balance
            int year = leave.getStartDate().getYear();
            LeaveBalance balance = getOrCreateLeaveBalance(leave.getEmployee().getId(), year);
            long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            
            if (leave.getLeaveType() == LeaveApplication.LeaveType.SICK_LEAVE) {
                balance.setUsedSickLeaves(Math.max(0, balance.getUsedSickLeaves() - (int)days));
            } else if (leave.getLeaveType() == LeaveApplication.LeaveType.CASUAL_LEAVE) {
                balance.setUsedCasualLeaves(balance.getUsedCasualLeaves() + (int)days);
            }
            leaveBalanceRepository.save(balance);
        } else if (leave.getStatus() == LeaveApplication.LeaveStatus.APPROVED && status != LeaveApplication.LeaveStatus.APPROVED) {
            // Reverting approval, subtract from balance
            int year = leave.getStartDate().getYear();
            LeaveBalance balance = getOrCreateLeaveBalance(leave.getEmployee().getId(), year);
            long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            
            if (leave.getLeaveType() == LeaveApplication.LeaveType.SICK_LEAVE) {
                balance.setUsedSickLeaves(Math.max(0, balance.getUsedSickLeaves() - (int)days));
            } else {
                balance.setUsedCasualLeaves(Math.max(0, balance.getUsedCasualLeaves() - (int)days));
            }
            leaveBalanceRepository.save(balance);
        }

        leave.setStatus(status);
        return leaveApplicationRepository.save(leave);
    }
    @Transactional(readOnly = true)
    public java.util.List<com.levanto.flooring.dto.CalendarDayDto> getAttendanceCalendar(Long employeeId, int year, int month) {
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByDateBetweenAndEmployeeIdOptional(start, end, employeeId);
        List<LeaveApplication> leaves = leaveApplicationRepository.findApprovedLeavesInPeriod(employeeId, start, end);
        List<Holiday> holidays = holidayRepository.findByDateBetween(start, end);
        
        java.util.List<java.time.DayOfWeek> weeklyOffDays = companySettingsService.getSettings().getWeeklyOffDaysList();

        java.util.List<com.levanto.flooring.dto.CalendarDayDto> calendar = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            final LocalDate current = d;
            Attendance att = attendances.stream().filter(a -> a.getDate().equals(current)).findFirst().orElse(null);
            LeaveApplication leave = leaves.stream().filter(l -> !current.isBefore(l.getStartDate()) && !current.isAfter(l.getEndDate())).findFirst().orElse(null);
            Holiday holiday = holidays.stream().filter(h -> h.getDate().equals(current)).findFirst().orElse(null);

            calendar.add(com.levanto.flooring.dto.CalendarDayDto.builder()
                .date(current)
                .attendance(att)
                .approvedLeave(leave)
                .isLeave(leave != null)
                .isWeekend(weeklyOffDays.contains(current.getDayOfWeek()))
                .isFuture(current.isAfter(today))
                .isHoliday(holiday != null)
                .holidayName(holiday != null ? holiday.getName() : null)
                .build());
        }
        return calendar;
    }


}
