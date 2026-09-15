package com.trasua.api;

import com.trasua.api.dto.AttendanceRequest;
import com.trasua.api.dto.AttendanceResponse;
import com.trasua.api.dto.DailySummaryResponse;
import com.trasua.service.AuthService;
import com.trasua.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ExpenseService expenses;
    private final AuthService auth;

    public AttendanceController(ExpenseService expenses, AuthService auth) {
        this.expenses = expenses;
        this.auth = auth;
    }

    @GetMapping
    public List<AttendanceResponse> list(@RequestHeader("Authorization") String authorization,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return expenses.listAttendances(date == null ? LocalDate.now(VIETNAM) : date, auth.currentMember(authorization));
    }

    @GetMapping("/daily-summary")
    public DailySummaryResponse summary(@RequestHeader("Authorization") String authorization,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return expenses.dailySummary(date == null ? LocalDate.now(VIETNAM) : date, auth.currentMember(authorization));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse create(@RequestHeader("Authorization") String authorization,
                                      @Valid @RequestBody AttendanceRequest request) {
        return expenses.createAttendance(request, auth.currentMember(authorization));
    }
}
