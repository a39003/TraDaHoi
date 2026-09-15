package com.trasua.service;

import com.trasua.domain.ChatMessage;
import com.trasua.repository.ChatAttachmentRepository;
import com.trasua.repository.ChatMessageRepository;
import com.trasua.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChatMaintenanceService {
    private static final Logger log = LoggerFactory.getLogger(ChatMaintenanceService.class);
    private static final int DELETE_BATCH_SIZE = 200;

    private final ChatMessageRepository messages;
    private final ChatAttachmentRepository attachments;
    private final MemberRepository members;
    private final ChatMediaStorageService mediaStorage;
    private final int retentionMonths;

    public ChatMaintenanceService(ChatMessageRepository messages,
                                  ChatAttachmentRepository attachments,
                                  MemberRepository members,
                                  ChatMediaStorageService mediaStorage,
                                  @Value("${app.chat.retention-months:6}") int retentionMonths) {
        this.messages = messages;
        this.attachments = attachments;
        this.members = members;
        this.mediaStorage = mediaStorage;
        this.retentionMonths = Math.max(1, retentionMonths);
    }

    @Transactional
    public CleanupResult cleanup() {
        Instant cutoff = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(retentionMonths).toInstant();
        Set<String> expiredFileKeys = new HashSet<>();
        int deletedMessages = 0;

        while (true) {
            List<Long> ids = messages.findExpiredIds(cutoff, PageRequest.of(0, DELETE_BATCH_SIZE));
            if (ids.isEmpty()) break;
            List<ChatMessage> batch = messages.findWithAttachmentsByIdIn(ids);
            batch.forEach(message -> message.getAttachments().forEach(item -> expiredFileKeys.add(item.getStorageKey())));
            messages.deleteAll(batch);
            messages.flush();
            deletedMessages += batch.size();
        }

        Set<String> referencedKeys = new HashSet<>(attachments.findAllStorageKeys());
        members.findAllAvatarUrls().stream().map(this::avatarStorageKey).filter(value -> value != null && !value.isBlank())
                .forEach(referencedKeys::add);
        int finalDeletedMessages = deletedMessages;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                expiredFileKeys.forEach(mediaStorage::deleteQuietly);
                int orphanFiles = mediaStorage.deleteOrphans(referencedKeys, Duration.ofHours(24));
                log.info("Chat cleanup complete: {} expired messages and {} orphan files removed", finalDeletedMessages, orphanFiles);
            }
        });
        return new CleanupResult(deletedMessages, cutoff);
    }

    private String avatarStorageKey(String avatarUrl) {
        String prefix = "/api/members/avatar/";
        int index = avatarUrl == null ? -1 : avatarUrl.indexOf(prefix);
        return index < 0 ? null : avatarUrl.substring(index + prefix.length());
    }

    public record CleanupResult(int deletedMessages, Instant cutoff) {}
}
