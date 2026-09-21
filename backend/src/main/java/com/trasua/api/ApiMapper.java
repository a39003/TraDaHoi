package com.trasua.api;

import com.trasua.api.dto.AttendanceResponse;
import com.trasua.api.dto.ChatAttachmentResponse;
import com.trasua.api.dto.ChatMessageResponse;
import com.trasua.api.dto.ChatReactionResponse;
import com.trasua.api.dto.ChatReplyPreviewResponse;
import com.trasua.api.dto.ExpenseItemResponse;
import com.trasua.api.dto.ExpenseResponse;
import com.trasua.api.dto.MemberBriefResponse;
import com.trasua.api.dto.MemberResponse;
import com.trasua.api.dto.NotificationResponse;
import com.trasua.api.dto.PaymentQrResponse;
import com.trasua.api.dto.SettlementBalanceResponse;
import com.trasua.api.dto.TransferResponse;
import com.trasua.domain.ChatMessage;
import com.trasua.domain.ChatAttachment;
import com.trasua.domain.DrinkOrder;
import com.trasua.domain.Member;
import com.trasua.domain.Notification;
import com.trasua.domain.OrderItem;
import com.trasua.domain.SettlementBalance;
import com.trasua.domain.SettlementTransfer;
import com.trasua.support.QrImageUrlFactory;

import java.time.LocalDate;
import java.util.List;

public final class ApiMapper {
    private ApiMapper() {
    }

    public static MemberResponse member(Member entity) {
        return new MemberResponse(entity.getId(), entity.getDisplayName(), entity.getEmail(), entity.getBankName(),
                entity.getBankBin(), entity.getAccountNumber(), entity.getAccountName(), entity.getQrCodeUrl(), entity.getAvatarUrl(),
                entity.isActive(), entity.getRole(), entity.getCreatedAt(), entity.getLastChatReadMessageId());
    }

    public static MemberBriefResponse brief(Member entity) {
        return new MemberBriefResponse(entity.getId(), entity.getDisplayName(), entity.getAvatarUrl());
    }

    public static ExpenseResponse expense(DrinkOrder entity) {
        List<ExpenseItemResponse> items = entity.getItems().stream()
                .map(item -> new ExpenseItemResponse(item.getId(), brief(item.getConsumer()), item.getDrinkName(),
                        item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))
                .toList();
        return new ExpenseResponse(entity.getId(), entity.getOrderDate(), brief(entity.getPayer()), brief(entity.getCreatedBy()),
                entity.getTotalAmount(), entity.getSplitMode(), entity.getNote(), items, entity.getCreatedAt());
    }

    public static AttendanceResponse attendance(DrinkOrder order, OrderItem item) {
        return new AttendanceResponse(item.getId(), order.getId(), order.getOrderDate(), brief(item.getConsumer()),
                brief(order.getPayer()), item.getDrinkName(), item.getLineTotal(), item.getQuantity());
    }

    public static ChatMessageResponse chatMessage(ChatMessage entity) {
        ChatReplyPreviewResponse reply = entity.getReplyTo() == null ? null
                : new ChatReplyPreviewResponse(entity.getReplyTo().getId(), brief(entity.getReplyTo().getSender()),
                entity.getReplyTo().getDeletedAt() == null ? entity.getReplyTo().getContent() : "Tin nhan da bi xoa",
                entity.getReplyTo().getDeletedAt() != null);
        return new ChatMessageResponse(entity.getId(), brief(entity.getSender()),
                entity.getDeletedAt() == null ? entity.getContent() : "", entity.getDeletedAt() == null
                ? entity.getAttachments().stream().map(ApiMapper::chatAttachment).toList() : List.of(),
                entity.getDeletedAt() == null ? entity.getReactions().stream()
                        .map(item -> new ChatReactionResponse(item.getId(), brief(item.getMember()), item.getEmoji(), item.getCreatedAt()))
                        .toList() : List.of(),
                reply, entity.getDeletedAt() != null, entity.getCreatedAt());
    }

    public static ChatAttachmentResponse chatAttachment(ChatAttachment entity) {
        return new ChatAttachmentResponse(entity.getId(), "/api/chat/messages/attachments/" + entity.getId() + "/content",
                entity.getOriginalFilename(), entity.getContentType(), entity.getByteSize());
    }

    public static NotificationResponse notification(Notification entity) {
        return new NotificationResponse(entity.getId(), entity.getType(), entity.getTitle(), entity.getBody(),
                entity.getSettlement() == null ? null : entity.getSettlement().getId(),
                entity.getTransfer() == null ? null : entity.getTransfer().getId(), entity.getReadAt(), entity.getCreatedAt());
    }

    public static SettlementBalanceResponse balance(SettlementBalance entity) {
        return new SettlementBalanceResponse(member(entity.getMember()), entity.getConsumedAmount(), entity.getPaidAmount(), entity.getNetAmount());
    }

    public static TransferResponse transfer(SettlementTransfer entity, LocalDate weekStart) {
        String content = transferContent(weekStart, entity.getId());
        return new TransferResponse(entity.getId(), member(entity.getDebtor()), member(entity.getCreditor()), entity.getAmount(),
                entity.getStatus(), entity.getPaidAt(), content, QrImageUrlFactory.forTransfer(entity.getCreditor(), entity.getAmount(), content));
    }

    public static PaymentQrResponse paymentQr(SettlementTransfer entity, LocalDate weekStart) {
        String content = transferContent(weekStart, entity.getId());
        Member recipient = entity.getCreditor();
        return new PaymentQrResponse(entity.getId(), QrImageUrlFactory.forTransfer(recipient, entity.getAmount(), content),
                recipient.getBankName(), recipient.getAccountNumber(), recipient.getAccountName(), entity.getAmount(), content);
    }

    private static String transferContent(LocalDate weekStart, Long transferId) {
        return "TRA DA " + weekStart + " #" + transferId;
    }
}
