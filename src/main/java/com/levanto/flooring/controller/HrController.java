package com.levanto.flooring.controller;

import com.levanto.flooring.entity.Attendance;
import com.levanto.flooring.entity.Employee;
import com.levanto.flooring.entity.LeaveApplication;
import com.levanto.flooring.service.AttendancePdfService;
import com.levanto.flooring.service.EmployeeService;
import com.levanto.flooring.service.HrService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;
    private final EmployeeService employeeService;
    private final AttendancePdfService attendancePdfService;

    // --- Attendance Ledger ---

    @GetMapping("/attendance")
    public String viewAttendanceLedger(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId,
            Model model) {
        
        LocalDate today = LocalDate.now();
        int targetYear = (year != null) ? year : today.getYear();
        int targetMonth = (month != null) ? month : today.getMonthValue();
        
        List<Employee> employees = employeeService.getActive();
        Long targetEmployeeId = (employeeId != null) ? employeeId : (!employees.isEmpty() ? employees.get(0).getId() : null);

        if (targetEmployeeId != null) {
            List<com.levanto.flooring.dto.CalendarDayDto> calendar = hrService.getAttendanceCalendar(targetEmployeeId, targetYear, targetMonth);
            model.addAttribute("calendar", calendar);
            if (!calendar.isEmpty()) {
                model.addAttribute("startOffset", calendar.get(0).getDate().getDayOfWeek().getValue() - 1);
            }

            int totalWorkingDays = 0;
            int totalLeaves = 0;
            int totalPresent = 0;

            LocalDate endOfCalculation;
            if (targetYear == today.getYear() && targetMonth == today.getMonthValue()) {
                endOfCalculation = today;
            } else {
                endOfCalculation = java.time.YearMonth.of(targetYear, targetMonth).atEndOfMonth();
            }

            for (com.levanto.flooring.dto.CalendarDayDto day : calendar) {
                if (!day.getDate().isAfter(endOfCalculation)) {
                    if (!day.isWeekend() && !day.isHoliday()) {
                        totalWorkingDays++;
                        if (day.isLeave()) {
                            totalLeaves++;
                        } else if (day.getAttendance() != null && 
                                  (day.getAttendance().getClockInTime() != null || 
                                   day.getAttendance().getStatus() == com.levanto.flooring.entity.Attendance.AttendanceStatus.PRESENT || 
                                   day.getAttendance().getStatus() == com.levanto.flooring.entity.Attendance.AttendanceStatus.HALF_DAY)) {
                            totalPresent++;
                        }
                    }
                }
            }

            model.addAttribute("statWorkingDays", totalWorkingDays);
            model.addAttribute("statLeaves", totalLeaves);
            model.addAttribute("statPresent", totalPresent);
            model.addAttribute("statAbsent", totalWorkingDays - totalLeaves - totalPresent);
        }

        model.addAttribute("employees", employees);
        model.addAttribute("selectedYear", targetYear);
        model.addAttribute("selectedMonth", targetMonth);
        model.addAttribute("selectedEmployeeId", targetEmployeeId);
        model.addAttribute("years", java.util.Arrays.asList(targetYear - 1, targetYear, targetYear + 1));
        model.addAttribute("months", java.time.Month.values());
        
        return "hr/attendance-ledger";
    }

    @GetMapping("/attendance/report")
    public String viewAttendanceReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        
        if (year == null) year = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();

        org.springframework.data.domain.Page<com.levanto.flooring.dto.AttendanceReportDto> attendancesPage = hrService.getAttendanceReportPaginated(year, month, employeeId, page, size);
        
        model.addAttribute("attendancesPage", attendancesPage);
        model.addAttribute("attendances", attendancesPage.getContent());
        model.addAttribute("employees", employeeService.getAll());
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("currentPage", page);
        
        return "hr/attendance-report";
    }

    @GetMapping("/attendance/report/export/pdf")
    public ResponseEntity<byte[]> exportAttendanceReportPdf(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId) {
        
        if (year == null) year = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();

        List<com.levanto.flooring.dto.AttendanceReportDto> attendances = hrService.getAttendanceReport(year, month, employeeId);
        String employeeName = null;
        if (employeeId != null) {
            try {
                Employee emp = employeeService.getById(employeeId);
                employeeName = emp.getName();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        byte[] pdf = attendancePdfService.generate(attendances, year, month, employeeName);
        
        String name = "Attendance_Report_" + String.format("%02d", month) + "_" + year + ".pdf";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
            .body(pdf);
    }

    @PostMapping("/attendance/clock-in")
    public String clockIn(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            hrService.clockIn(employeeId, date);
            redirectAttributes.addFlashAttribute("success", "Clocked in successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hr/attendance?year=" + date.getYear() + "&month=" + date.getMonthValue() + "&employeeId=" + employeeId;
    }

    @PostMapping("/attendance/clock-out")
    public String clockOut(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            hrService.clockOut(employeeId, date);
            redirectAttributes.addFlashAttribute("success", "Clocked out successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hr/attendance?year=" + date.getYear() + "&month=" + date.getMonthValue() + "&employeeId=" + employeeId;
    }

    @PostMapping("/attendance/manual-correction")
    public String manualCorrection(
            @RequestParam Long attendanceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime clockInTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime clockOutTime,
            @RequestParam Attendance.AttendanceStatus status,
            @RequestParam(required = false) String adminNotes,
            RedirectAttributes redirectAttributes) {
        
        try {
            hrService.manualCorrection(attendanceId, clockInTime, clockOutTime, status, adminNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Attendance manually corrected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error correcting attendance.");
        }
        return "redirect:/hr/attendance";
    }



    // --- Leave Management ---

    @GetMapping("/leaves")
    public String viewLeaveManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        org.springframework.data.domain.Page<LeaveApplication> leavePage = hrService.getAllLeaveApplicationsPaginated(page, size);
        model.addAttribute("leavePage", leavePage);
        model.addAttribute("leaveApplications", leavePage.getContent());
        model.addAttribute("employees", employeeService.getAll());
        model.addAttribute("newApplication", new LeaveApplication());
        model.addAttribute("currentPage", page);
        return "hr/leave-management";
    }

    @ResponseBody
    @GetMapping("/leaves/balance")
    public org.springframework.http.ResponseEntity<?> getLeaveBalance(@RequestParam Long employeeId) {
        try {
            int year = LocalDate.now().getYear();
            com.levanto.flooring.entity.LeaveBalance balance = hrService.getOrCreateLeaveBalance(employeeId, year);
            return org.springframework.http.ResponseEntity.ok(
                com.levanto.flooring.dto.LeaveBalanceDto.builder()
                    .allocatedSickLeaves(balance.getAllocatedSickLeaves())
                    .usedSickLeaves(balance.getUsedSickLeaves())
                    .remainingSickLeaves(balance.getRemainingSickLeaves())
                    .allocatedCasualLeaves(balance.getAllocatedCasualLeaves())
                    .usedCasualLeaves(balance.getUsedCasualLeaves())
                    .remainingCasualLeaves(balance.getRemainingCasualLeaves())
                    .build()
            );
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body("Error fetching balance.");
        }
    }

    @PostMapping("/leaves/apply")
    public String applyForLeave(
            @RequestParam Long employeeId,
            @ModelAttribute LeaveApplication application,
            RedirectAttributes redirectAttributes) {
        try {
            hrService.applyForLeave(employeeId, application);
            redirectAttributes.addFlashAttribute("success", "Leave application submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Error submitting leave application.");
        }
        return "redirect:/hr/leaves";
    }

    @PostMapping("/leaves/{id}/status")
    public String updateLeaveStatus(
            @PathVariable Long id,
            @RequestParam LeaveApplication.LeaveStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            hrService.updateLeaveStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Leave status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating leave status.");
        }
        return "redirect:/hr/leaves";
    }
}
