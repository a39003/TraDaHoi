package com.trasua.service;

import com.trasua.api.ApiMapper;
import com.trasua.api.dto.AttendanceRequest;
import com.trasua.api.dto.AttendanceResponse;
import com.trasua.api.dto.DailySummaryResponse;
import com.trasua.api.dto.ExpenseItemRequest;
import com.trasua.api.dto.ExpenseRequest;
import com.trasua.api.dto.ExpenseResponse;
import com.trasua.domain.DrinkOrder;
import com.trasua.domain.Member;
import com.trasua.domain.OrderItem;
import com.trasua.domain.SettlementStatus;
import com.trasua.repository.DrinkOrderRepository;
import com.trasua.repository.DrinkCatalogRepository;
import com.trasua.repository.WeeklySettlementRepository;
import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import com.trasua.support.WeekUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class ExpenseService {
    private final DrinkOrderRepository orders;
    private final WeeklySettlementRepository settlements;
    private final MemberService memberService;
    private final AttendanceSettingsService attendanceSettings;
    private final DrinkCatalogRepository catalog;

    public ExpenseService(DrinkOrderRepository orders, WeeklySettlementRepository settlements,
                          MemberService memberService, AttendanceSettingsService attendanceSettings,
                          DrinkCatalogRepository catalog) {
        this.orders = orders;
        this.settlements = settlements;
        this.memberService = memberService;
        this.attendanceSettings = attendanceSettings;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(LocalDate date, Member actor) {
        return visible(orders.findDetailedByOrderDate(date), actor).stream().map(ApiMapper::expense).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(LocalDate from, LocalDate to, Member actor) {
        return visible(orders.findDetailedBetween(from, to), actor).stream().map(ApiMapper::expense).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> listAttendances(LocalDate date, Member actor) {
        return visible(orders.findDetailedByOrderDate(date), actor).stream()
                .flatMap(order -> order.getItems().stream().filter(item -> canSeeItem(item, actor))
                        .map(item -> ApiMapper.attendance(order, item)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DailySummaryResponse dailySummary(LocalDate date, Member actor) {
        List<DrinkOrder> dayOrders = visible(orders.findDetailedByOrderDate(date), actor);
        List<ExpenseResponse> expenseResponses = dayOrders.stream().map(ApiMapper::expense).toList();
        List<AttendanceResponse> attendances = dayOrders.stream()
                .flatMap(order -> order.getItems().stream().filter(item -> canSeeItem(item, actor))
                        .map(item -> ApiMapper.attendance(order, item)))
                .toList();
        long total = attendances.stream().mapToLong(AttendanceResponse::amount).sum();
        int drinkCount = attendances.stream().mapToInt(AttendanceResponse::quantity).sum();
        return new DailySummaryResponse(date, total, drinkCount, attendances, expenseResponses);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Member actor) {
        assertWeekOpen(request.orderDate());
        assertDailyEditable(request.orderDate(), actor);
        assertNoDuplicateAttendance(request, null);
        DrinkOrder order = build(request, new DrinkOrder());
        order.setCreatedBy(actor);
        orders.save(order);
        return ApiMapper.expense(order);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Member actor) {
        DrinkOrder order = orders.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điểm danh " + id));
        assertWeekOpen(order.getOrderDate());
        assertWeekOpen(request.orderDate());
        assertOwnerOrAdmin(order, actor);
        assertDailyEditable(order.getOrderDate(), actor);
        assertDailyEditable(request.orderDate(), actor);
        assertNoDuplicateAttendance(request, order.getId());
        build(request, order);
        return ApiMapper.expense(order);
    }

    @Transactional
    public void deleteExpense(Long id, Member actor) {
        DrinkOrder order = orders.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điểm danh " + id));
        assertWeekOpen(order.getOrderDate());
        assertOwnerOrAdmin(order, actor);
        assertDailyEditable(order.getOrderDate(), actor);
        orders.delete(order);
    }

    @Transactional
    public AttendanceResponse createAttendance(AttendanceRequest request, Member actor) {
        ExpenseRequest expenseRequest = new ExpenseRequest(request.date(), request.payerMemberId(), "ITEMIZED",
                request.note(), List.of(new ExpenseItemRequest(request.memberId(), request.drinkName(), request.amount(), 1)));
        ExpenseResponse expense = createExpense(expenseRequest, actor);
        var item = expense.items().get(0);
        return new AttendanceResponse(item.id(), expense.id(), expense.orderDate(), item.consumer(), expense.payer(),
                item.drinkName(), item.lineTotal(), item.quantity());
    }

    private DrinkOrder build(ExpenseRequest request, DrinkOrder order) {
        Member payer = memberService.requireActive(request.payerMemberId());
        String splitMode = normalizeSplitMode(request.splitMode());
        validateCatalogPrices(request.items(), splitMode);
        List<OrderItem> items = new ArrayList<>();
        long total = 0;
        for (ExpenseItemRequest itemRequest : request.items()) {
            Member consumer = memberService.requireActive(itemRequest.consumerMemberId());
            OrderItem item = new OrderItem();
            item.setConsumer(consumer);
            item.setDrinkName(itemRequest.drinkName().trim());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setQuantity(itemRequest.quantity());
            try {
                item.setLineTotal(Math.multiplyExact(itemRequest.unitPrice(), itemRequest.quantity()));
                total = Math.addExact(total, item.getLineTotal());
            } catch (ArithmeticException error) {
                throw new BusinessRuleException("Số tiền đồ uống vượt giới hạn cho phép");
            }
            items.add(item);
        }
        order.setOrderDate(request.orderDate());
        order.setPayer(payer);
        order.setSplitMode(splitMode);
        order.setNote(blankToNull(request.note()));
        order.setTotalAmount(total);
        order.replaceItems(items);
        return order;
    }

    /**
     * One member may order several drinks in one order, but may not be added
     * to another order on the same date. This also protects the API when two
     * members submit from different devices.
     */
    private void assertNoDuplicateAttendance(ExpenseRequest request, Long excludedOrderId) {
        Set<Long> requestedMemberIds = new LinkedHashSet<>();
        for (ExpenseItemRequest item : request.items()) {
            requestedMemberIds.add(item.consumerMemberId());
        }

        Set<String> duplicatedMembers = new LinkedHashSet<>();
        for (DrinkOrder dayOrder : orders.findDetailedByOrderDate(request.orderDate())) {
            if (excludedOrderId != null && excludedOrderId.equals(dayOrder.getId())) continue;
            for (OrderItem existingItem : dayOrder.getItems()) {
                if (requestedMemberIds.contains(existingItem.getConsumer().getId())) {
                    duplicatedMembers.add(existingItem.getConsumer().getDisplayName());
                }
            }
        }

        if (!duplicatedMembers.isEmpty()) {
            throw new BusinessRuleException("Thành viên " + String.join(", ", duplicatedMembers)
                    + " đã được điểm danh trong ngày này. Hãy xem hoặc sửa đơn đã có thay vì tạo thêm.");
        }
    }

    private void validateCatalogPrices(List<ExpenseItemRequest> items, String splitMode) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ExpenseItemRequest item : items) {
            var drink = catalog.findByNameIgnoreCase(item.drinkName().trim())
                    .filter(value -> value.isActive())
                    .orElseThrow(() -> new BusinessRuleException("Đồ uống " + item.drinkName() + " không còn trong danh sách"));
            if ("ITEMIZED".equals(splitMode) && item.unitPrice() != drink.getPrice()) {
                throw new BusinessRuleException("Giá " + drink.getName() + " phải là " + drink.getPrice() + " đ theo danh sách của Admin");
            }
            try {
                totals.merge(drink.getName(), Math.multiplyExact(item.unitPrice(), item.quantity()), Math::addExact);
            } catch (ArithmeticException error) {
                throw new BusinessRuleException("Số tiền đồ uống vượt giới hạn cho phép");
            }
        }
        if ("EVEN".equals(splitMode)) {
            totals.forEach((name, total) -> {
                long price = catalog.findByNameIgnoreCase(name).orElseThrow().getPrice();
                if (price == 0 ? total != 0 : total % price != 0) {
                    throw new BusinessRuleException("Tổng tiền chia cho " + name + " không khớp giá cố định của Admin");
                }
            });
        }
    }

    private List<DrinkOrder> visible(List<DrinkOrder> source, Member actor) {
        if ("ADMIN".equals(actor.getRole())) return source;
        return source.stream().filter(order -> order.getItems().stream()
                .anyMatch(item -> item.getConsumer().getId().equals(actor.getId()))).toList();
    }

    private boolean canSeeItem(OrderItem item, Member actor) {
        return "ADMIN".equals(actor.getRole()) || item.getConsumer().getId().equals(actor.getId());
    }

    private void assertOwnerOrAdmin(DrinkOrder order, Member actor) {
        if (!"ADMIN".equals(actor.getRole()) && !order.getCreatedBy().getId().equals(actor.getId())) {
            throw new BusinessRuleException("Bạn chỉ có thể sửa hoặc hủy điểm danh do chính mình tạo");
        }
    }

    private void assertDailyEditable(LocalDate date, Member actor) {
        if ("ADMIN".equals(actor.getRole())) return;
        LocalDate today = LocalDate.now(AttendanceSettingsService.VIETNAM);
        if (!date.equals(today)) {
            throw new BusinessRuleException("Thành viên chỉ được tạo hoặc sửa điểm danh của hôm nay");
        }
        LocalTime cutoff = attendanceSettings.cutoffTime();
        if (!LocalTime.now(AttendanceSettingsService.VIETNAM).isBefore(cutoff)) {
            throw new BusinessRuleException("Điểm danh hôm nay đã khóa lúc " + cutoff.toString().substring(0, 5));
        }
    }

    private String normalizeSplitMode(String value) {
        if (value == null || value.isBlank()) return "ITEMIZED";
        String normalized = value.trim().toUpperCase();
        if (!normalized.equals("ITEMIZED") && !normalized.equals("EVEN")) {
            throw new BusinessRuleException("Kiểu chia tiền phải là chia đều hoặc chia theo từng món");
        }
        return normalized;
    }

    private void assertWeekOpen(LocalDate date) {
        settlements.findByWeekStart(WeekUtils.mondayOf(date))
                .filter(settlement -> settlement.getStatus() == SettlementStatus.FINALIZED)
                .ifPresent(settlement -> {
                    throw new BusinessRuleException("Tuần " + settlement.getWeekStart() + " đã chốt, không thể sửa điểm danh");
                });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
