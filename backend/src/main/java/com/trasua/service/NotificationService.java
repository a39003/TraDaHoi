package com.trasua.service;

import com.trasua.api.ApiMapper;
import com.trasua.api.dto.NotificationResponse;
import com.trasua.domain.Notification;
import com.trasua.domain.NotificationType;
import com.trasua.domain.Member;
import com.trasua.repository.NotificationRepository;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForMember(Long memberId) {
        return notifications.findDetailedByRecipientId(memberId).stream().map(ApiMapper::notification).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long memberId) {
        Notification notification = notifications.findOwnedById(id, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo " + id));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return ApiMapper.notification(notification);
    }

    @Transactional
    public void delete(Long id, Long memberId) {
        Notification notification = notifications.findOwnedById(id, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo " + id));
        notifications.delete(notification);
    }

    @Transactional
    public void createMention(Member recipient, Member sender, String content) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.MENTION);
        notification.setTitle(sender.getDisplayName() + " đã nhắc đến bạn");
        String preview = content.length() > 180 ? content.substring(0, 180) + "…" : content;
        notification.setBody(preview);
        notifications.save(notification);
    }
}
