package com.trasua.api;

import com.trasua.api.dto.ExpenseRequest;
import com.trasua.api.dto.ExpenseResponse;
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
@RequestMapping("/api/expenses")
public class ExpenseController {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ExpenseService expenses;
    private final AuthService auth;

    public ExpenseController(ExpenseService expenses, AuthService auth) {
        this.expenses = expenses;
        this.auth = auth;
    }

    @GetMapping
    public List<ExpenseResponse> list(@RequestHeader("Authorization") String authorization,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return expenses.listExpenses(date == null ? LocalDate.now(VIETNAM) : date, auth.currentMember(authorization));
    }

    @GetMapping("/range")
    public List<ExpenseResponse> range(@RequestHeader("Authorization") String authorization,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return expenses.listExpenses(from, to, auth.currentMember(authorization));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@RequestHeader("Authorization") String authorization,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenses.createExpense(request, auth.currentMember(authorization));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@RequestHeader("Authorization") String authorization, @PathVariable Long id,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenses.updateExpense(id, request, auth.currentMember(authorization));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        expenses.deleteExpense(id, auth.currentMember(authorization));
    }
}
