package com.trasua.service;

import com.trasua.api.ApiMapper;
import com.trasua.api.dto.PaymentQrResponse;
import com.trasua.api.dto.SettlementResponse;
import com.trasua.api.dto.TransferResponse;
import com.trasua.domain.DrinkOrder;
import com.trasua.domain.Member;
import com.trasua.domain.Notification;
import com.trasua.domain.NotificationType;
import com.trasua.domain.OrderItem;
import com.trasua.domain.SettlementBalance;
import com.trasua.domain.SettlementStatus;
import com.trasua.domain.SettlementTransfer;
import com.trasua.domain.TransferStatus;
import com.trasua.domain.WeeklyNotificationSettings;
import com.trasua.domain.WeeklySettlement;
import com.trasua.repository.DrinkOrderRepository;
import com.trasua.repository.MemberRepository;
import com.trasua.repository.NotificationRepository;
import com.trasua.repository.SettlementBalanceRepository;
import com.trasua.repository.SettlementTransferRepository;
import com.trasua.repository.WeeklySettlementRepository;
import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import com.trasua.support.WeekUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeeklySettlementService {
    private final WeeklySettlementRepository settlements;
    private final SettlementBalanceRepository balances;
    private final SettlementTransferRepository transfers;
    private final DrinkOrderRepository orders;
    private final MemberRepository members;
    private final NotificationRepository notifications;
    private final WeeklyNotificationSettingsService notificationSettings;

    public WeeklySettlementService(WeeklySettlementRepository settlements,
                                   SettlementBalanceRepository balances,
                                   SettlementTransferRepository transfers,
                                   DrinkOrderRepository orders,
                                   MemberRepository members,
                                   NotificationRepository notifications,
                                   WeeklyNotificationSettingsService notificationSettings) {
        this.settlements = settlements;
        this.balances = balances;
        this.transfers = transfers;
        this.orders = orders;
        this.members = members;
        this.notifications = notifications;
        this.notificationSettings = notificationSettings;
    }

    @Transactional
    public SettlementResponse calculateWeek(LocalDate anyDateInWeek) {
        return toResponse(calculateDraft(WeekUtils.mondayOf(anyDateInWeek)));
    }

    @Transactional
    public SettlementResponse getWeek(LocalDate anyDateInWeek) {
        LocalDate weekStart = WeekUtils.mondayOf(anyDateInWeek);
        WeeklySettlement settlement = settlements.findByWeekStart(weekStart).orElseGet(() -> calculateDraft(weekStart));
        return toResponse(settlement);
    }

    @Transactional
    public SettlementResponse finalizeWeek(LocalDate anyDateInWeek) {
        LocalDate weekStart = WeekUtils.mondayOf(anyDateInWeek);
        WeeklySettlement existing = settlements.findByWeekStart(weekStart).orElse(null);
        if (existing != null && existing.getStatus() == SettlementStatus.FINALIZED) {
            return toResponse(existing);
        }

        WeeklySettlement settlement = calculateDraft(weekStart);
        settlement.setStatus(SettlementStatus.FINALIZED);
        settlement.setFinalizedAt(Instant.now());
        WeeklyNotificationSettings settings = notificationSettings.getEntity();

        Map<Long, Long> reimbursementByCreditor = new HashMap<>();
        Map<Long, Member> creditorMembers = new HashMap<>();
        Map<Long, List<SettlementTransfer>> transfersByCreditor = new LinkedHashMap<>();
        for (SettlementTransfer transfer : transfers.findDetailedBySettlementId(settlement.getId())) {
            reimbursementByCreditor.merge(transfer.getCreditor().getId(), transfer.getAmount(), Math::addExact);
            creditorMembers.put(transfer.getCreditor().getId(), transfer.getCreditor());
            transfersByCreditor.computeIfAbsent(transfer.getCreditor().getId(), ignored -> new ArrayList<>()).add(transfer);

            Notification notification = new Notification();
            notification.setRecipient(transfer.getDebtor());
            notification.setSettlement(settlement);
            notification.setTransfer(transfer);
            notification.setType(NotificationType.SETTLEMENT_READY);
            notification.setTitle(render(settings.getDebtorTitle(), transfer.getAmount(),
                    transfer.getCreditor().getDisplayName(), settlement));
            String debtorBody = render(settings.getDebtorBody(), transfer.getAmount(),
                    transfer.getCreditor().getDisplayName(), settlement)
                    + "\nNgười nhận: " + memberLabel(transfer.getCreditor()) + "."
                    + "\nSố tiền cần chuyển: " + money(transfer.getAmount()) + ".";
            notification.setBody(limit(debtorBody, 1000));
            notifications.save(notification);
        }

        for (Map.Entry<Long, Long> entry : reimbursementByCreditor.entrySet()) {
            Member creditor = creditorMembers.get(entry.getKey());
            Notification notification = new Notification();
            notification.setRecipient(creditor);
            notification.setSettlement(settlement);
            notification.setType(NotificationType.SETTLEMENT_READY);
            notification.setTitle(render(settings.getCreditorTitle(), entry.getValue(), creditor.getDisplayName(), settlement));
            StringBuilder creditorBody = new StringBuilder(render(settings.getCreditorBody(), entry.getValue(),
                    creditor.getDisplayName(), settlement));
            creditorBody.append("\nChi tiết thành viên cần trả cho bạn:");
            for (SettlementTransfer transfer : transfersByCreditor.getOrDefault(entry.getKey(), List.of())) {
                creditorBody.append("\n• ").append(memberLabel(transfer.getDebtor()))
                        .append(": ").append(money(transfer.getAmount()));
            }
            creditorBody.append("\nTổng tiền bạn sẽ nhận: ").append(money(entry.getValue())).append(".");
            notification.setBody(limit(creditorBody.toString(), 1000));
            notifications.save(notification);
        }
        return toResponse(settlement);
    }

    @Transactional
    public TransferResponse markTransferPaid(Long transferId) {
        SettlementTransfer transfer = transfers.findDetailedById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu chuyển khoản " + transferId));
        if (transfer.getSettlement().getStatus() != SettlementStatus.FINALIZED) {
            throw new BusinessRuleException("Tuần này chưa được chốt");
        }
        if (transfer.getStatus() == TransferStatus.PAID) {
            return ApiMapper.transfer(transfer, transfer.getSettlement().getWeekStart());
        }
        transfer.setStatus(TransferStatus.PAID);
        transfer.setPaidAt(Instant.now());

        Notification notification = new Notification();
        notification.setRecipient(transfer.getCreditor());
        notification.setSettlement(transfer.getSettlement());
        notification.setTransfer(transfer);
        notification.setType(NotificationType.PAYMENT_RECEIVED);
        notification.setTitle("Da xac nhan thanh toan");
        notification.setBody(transfer.getDebtor().getDisplayName() + " da xac nhan chuyen "
                + String.format("%,d d", transfer.getAmount()).replace(',', '.') + ".");
        notifications.save(notification);
        return ApiMapper.transfer(transfer, transfer.getSettlement().getWeekStart());
    }

    @Transactional(readOnly = true)
    public PaymentQrResponse paymentQr(Long transferId) {
        SettlementTransfer transfer = transfers.findDetailedById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu chuyển khoản " + transferId));
        return ApiMapper.paymentQr(transfer, transfer.getSettlement().getWeekStart());
    }

    private String render(String template, long amount, String creditor, WeeklySettlement settlement) {
        return notificationSettings.render(template, amount, creditor, settlement.getWeekStart().toString());
    }

    private String memberLabel(Member member) {
        return member.getDisplayName() + " (Mã thành viên #" + member.getId() + ")";
    }

    private String money(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private WeeklySettlement calculateDraft(LocalDate weekStart) {
        WeeklySettlement settlement = settlements.findByWeekStart(weekStart).orElseGet(() -> {
            WeeklySettlement fresh = new WeeklySettlement();
            fresh.setWeekStart(weekStart);
            fresh.setWeekEnd(weekStart.plusDays(5));
            fresh.setStatus(SettlementStatus.DRAFT);
            fresh.setTotalAmount(0);
            return settlements.save(fresh);
        });
        if (settlement.getStatus() == SettlementStatus.FINALIZED) return settlement;

        transfers.deleteBySettlementId(settlement.getId());
        balances.deleteBySettlementId(settlement.getId());

        Map<Long, Long> consumed = new HashMap<>();
        Map<Long, Long> paid = new HashMap<>();
        long total = 0;
        for (DrinkOrder order : orders.findDetailedBetween(weekStart, weekStart.plusDays(5))) {
            paid.merge(order.getPayer().getId(), order.getTotalAmount(), Math::addExact);
            total = Math.addExact(total, order.getTotalAmount());
            for (OrderItem item : order.getItems()) {
                consumed.merge(item.getConsumer().getId(), item.getLineTotal(), Math::addExact);
            }
        }
        settlement.setWeekEnd(weekStart.plusDays(5));
        settlement.setTotalAmount(total);

        List<SettlementBalance> generatedBalances = new ArrayList<>();
        List<BalancePosition> debtors = new ArrayList<>();
        List<BalancePosition> creditors = new ArrayList<>();
        for (Member member : members.findAll()) {
            long consumedAmount = consumed.getOrDefault(member.getId(), 0L);
            long paidAmount = paid.getOrDefault(member.getId(), 0L);
            long net = Math.subtractExact(paidAmount, consumedAmount);

            SettlementBalance balance = new SettlementBalance();
            balance.setSettlement(settlement);
            balance.setMember(member);
            balance.setConsumedAmount(consumedAmount);
            balance.setPaidAmount(paidAmount);
            balance.setNetAmount(net);
            generatedBalances.add(balance);
            if (net < 0) debtors.add(new BalancePosition(member, -net));
            else if (net > 0) creditors.add(new BalancePosition(member, net));
        }
        balances.saveAll(generatedBalances);

        List<SettlementTransfer> generatedTransfers = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            BalancePosition debtor = debtors.get(debtorIndex);
            BalancePosition creditor = creditors.get(creditorIndex);
            long amount = Math.min(debtor.remaining, creditor.remaining);
            SettlementTransfer transfer = new SettlementTransfer();
            transfer.setSettlement(settlement);
            transfer.setDebtor(debtor.member);
            transfer.setCreditor(creditor.member);
            transfer.setAmount(amount);
            transfer.setStatus(TransferStatus.PENDING);
            generatedTransfers.add(transfer);
            debtor.remaining -= amount;
            creditor.remaining -= amount;
            if (debtor.remaining == 0) debtorIndex++;
            if (creditor.remaining == 0) creditorIndex++;
        }
        if (debtorIndex != debtors.size() || creditorIndex != creditors.size()) {
            throw new BusinessRuleException("Không thể cân đối tổng tiền tuần; hãy kiểm tra lại các khoản chi");
        }
        transfers.saveAll(generatedTransfers);
        return settlement;
    }

    private SettlementResponse toResponse(WeeklySettlement settlement) {
        return new SettlementResponse(settlement.getId(), settlement.getWeekStart(), settlement.getWeekEnd(),
                settlement.getTotalAmount(), settlement.getStatus(), settlement.getFinalizedAt(),
                balances.findDetailedBySettlementId(settlement.getId()).stream().map(ApiMapper::balance).toList(),
                transfers.findDetailedBySettlementId(settlement.getId()).stream()
                        .map(transfer -> ApiMapper.transfer(transfer, settlement.getWeekStart())).toList());
    }

    private static final class BalancePosition {
        private final Member member;
        private long remaining;

        private BalancePosition(Member member, long remaining) {
            this.member = member;
            this.remaining = remaining;
        }
    }
}
