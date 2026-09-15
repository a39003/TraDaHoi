package com.trasua.api;

import com.trasua.api.dto.PaymentQrResponse;
import com.trasua.api.dto.SettlementResponse;
import com.trasua.api.dto.TransferResponse;
import com.trasua.service.WeeklySettlementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class WeeklySettlementController {
    private final WeeklySettlementService settlements;

    public WeeklySettlementController(WeeklySettlementService settlements) {
        this.settlements = settlements;
    }

    @GetMapping("/api/weeks/{weekStart}/settlement")
    public SettlementResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return settlements.getWeek(weekStart);
    }

    @PostMapping("/api/weeks/{weekStart}/settlement/calculate")
    public SettlementResponse calculate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return settlements.calculateWeek(weekStart);
    }

    @PostMapping("/api/weeks/{weekStart}/settlement/finalize")
    public SettlementResponse finalizeWeek(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return settlements.finalizeWeek(weekStart);
    }

    @PostMapping("/api/settlement-transfers/{id}/mark-paid")
    public TransferResponse markPaid(@PathVariable Long id) {
        return settlements.markTransferPaid(id);
    }

    @GetMapping("/api/settlement-transfers/{id}/payment-qr")
    public PaymentQrResponse paymentQr(@PathVariable Long id) {
        return settlements.paymentQr(id);
    }
}
