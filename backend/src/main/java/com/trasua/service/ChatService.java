package com.trasua.service;

import com.trasua.api.ApiMapper;
import com.trasua.api.dto.ChatMessageRequest;
import com.trasua.api.dto.ChatMessageResponse;
import com.trasua.api.dto.ChatUnreadResponse;
import com.trasua.api.dto.ChatReactionRequest;
import com.trasua.domain.ChatAttachment;
import com.trasua.domain.ChatMessage;
import com.trasua.domain.Member;
import com.trasua.domain.ChatReaction;
import com.trasua.repository.ChatAttachmentRepository;
import com.trasua.repository.ChatMessageRepository;
import com.trasua.repository.ChatReactionRepository;
import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ChatService {
    private static final int MAX_IMAGES_PER_MESSAGE = 4;
    private static final Set<String> ALLOWED_REACTIONS = Set.of("👍", "❤️", "😂", "😮", "😢", "🎉");

    private final ChatMessageRepository messages;
    private final ChatAttachmentRepository attachments;
    private final MemberService members;
    private final SimpMessagingTemplate messaging;
    private final ChatMediaStorageService mediaStorage;
    private final JdbcTemplate jdbc;
    private final ChatReactionRepository reactions;
    private final NotificationService notificationService;

    public ChatService(ChatMessageRepository messages,
                       ChatAttachmentRepository attachments,
                       MemberService members,
                       SimpMessagingTemplate messaging,
                       ChatMediaStorageService mediaStorage,
                       JdbcTemplate jdbc,
                       ChatReactionRepository reactions,
                       NotificationService notificationService) {
        this.messages = messages;
        this.attachments = attachments;
        this.members = members;
        this.messaging = messaging;
        this.mediaStorage = mediaStorage;
        this.jdbc = jdbc;
        this.reactions = reactions;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> list(Instant before, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        List<ChatMessage> result = before == null
                ? messages.findRecent(PageRequest.of(0, pageSize))
                : messages.findOlderThan(before, PageRequest.of(0, pageSize));
        List<ChatMessageResponse> output = new ArrayList<>(result.stream().map(ApiMapper::chatMessage).toList());
        Collections.reverse(output);
        return output;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> search(String query, Instant before, int limit) {
        String keyword = query == null ? "" : query.trim();
        if (keyword.length() < 2) throw new BusinessRuleException("Hãy nhập ít nhất 2 ký tự để tìm kiếm");
        if (keyword.length() > 100) throw new BusinessRuleException("Từ khóa tìm kiếm tối đa 100 ký tự");
        int pageSize = Math.max(1, Math.min(limit, 100));
        List<ChatMessage> result = before == null
                ? messages.searchRecent(keyword, PageRequest.of(0, pageSize))
                : messages.searchOlder(keyword, before, PageRequest.of(0, pageSize));
        List<ChatMessageResponse> output = new ArrayList<>(result.stream().map(ApiMapper::chatMessage).toList());
        Collections.reverse(output);
        return output;
    }

    @Transactional(readOnly = true)
    public ChatUnreadResponse unreadCount(Member actor) {
        Long lastReadId = actor.getLastChatReadMessageId();
        long count = messages.countBySenderIdNotAndIdGreaterThan(actor.getId(), lastReadId == null ? 0L : lastReadId);
        return new ChatUnreadResponse(count, lastReadId);
    }

    @Transactional
    public ChatUnreadResponse markRead(Member actor) {
        Long latestId = messages.findMaxId();
        members.markChatRead(actor.getId(), latestId);
        return new ChatUnreadResponse(0, latestId);
    }

    @Transactional
    public ChatMessageResponse send(ChatMessageRequest request, Member actor) {
        ChatMessage message = new ChatMessage();
        message.setSender(actor);
        message.setContent(request.content().trim());
        message.setReplyTo(findReply(request.replyToMessageId()));
        messages.save(message);
        notifyMentions(message);
        return publish(message);
    }

    @Transactional
    public ChatMessageResponse sendWithMedia(Member actor, String content, Long replyToMessageId, List<MultipartFile> images) {
        String normalizedContent = content == null ? "" : content.trim();
        List<MultipartFile> validImages = images == null ? List.of() : images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();
        if (normalizedContent.isBlank() && validImages.isEmpty()) {
            throw new BusinessRuleException("Tin nhắn cần có nội dung hoặc ít nhất một ảnh");
        }
        if (normalizedContent.length() > 2_000) {
            throw new BusinessRuleException("Tin nhắn tối đa 2.000 ký tự");
        }
        if (validImages.size() > MAX_IMAGES_PER_MESSAGE) {
            throw new BusinessRuleException("Mỗi tin nhắn chỉ được gửi tối đa 4 ảnh");
        }

        ChatMessage message = new ChatMessage();
        message.setSender(actor);
        message.setContent(normalizedContent);
        message.setReplyTo(findReply(replyToMessageId));

        List<String> storedKeys = new ArrayList<>();
        try {
            for (MultipartFile image : validImages) {
                ChatMediaStorageService.StoredChatMedia stored = mediaStorage.store(image);
                storedKeys.add(stored.storageKey());

                ChatAttachment attachment = new ChatAttachment();
                attachment.setStorageKey(stored.storageKey());
                attachment.setOriginalFilename(stored.originalFilename());
                attachment.setContentType(stored.contentType());
                attachment.setByteSize(stored.byteSize());
                message.addAttachment(attachment);
            }
            messages.saveAndFlush(message);
            notifyMentions(message);
            return publish(message);
        } catch (RuntimeException error) {
            storedKeys.forEach(mediaStorage::deleteQuietly);
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public ChatMediaContent attachmentContent(Long attachmentId) {
        ChatAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh chat " + attachmentId));
        return new ChatMediaContent(mediaStorage.load(attachment.getStorageKey()), attachment.getContentType(), attachment.getByteSize());
    }

    @Transactional
    public void delete(Long messageId, Member actor) {
        ChatMessage message = messages.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn " + messageId));
        boolean owner = message.getSender().getId().equals(actor.getId());
        if (!owner && !"ADMIN".equals(actor.getRole())) {
            throw new BusinessRuleException("Bạn không có quyền xóa tin nhắn này");
        }
        List<String> storedKeys = message.getAttachments().stream().map(ChatAttachment::getStorageKey).toList();
        // Dùng DELETE vật lý trực tiếp. Khóa ngoại reply_to_message_id sẽ tự
        // chuyển các câu trả lời sang NULL, còn ảnh đính kèm được cascade xóa.
        int deleted = jdbc.update("DELETE FROM chat_messages WHERE id = ?", messageId);
        if (deleted != 1) {
            throw new ResourceNotFoundException("Không tìm thấy tin nhắn " + messageId);
        }
        deleteMediaAfterCommit(storedKeys);
    }

    @Transactional
    public void clearGroupChat() {
        List<String> storedKeys = new ArrayList<>(attachments.findAllStorageKeys());
        jdbc.update("DELETE FROM chat_messages");
        deleteMediaAfterCommit(storedKeys);
    }

    @Transactional
    public ChatMessageResponse toggleReaction(Long messageId, ChatReactionRequest request, Member actor) {
        String emoji = request.emoji().trim();
        if (!ALLOWED_REACTIONS.contains(emoji)) {
            throw new BusinessRuleException("Cảm xúc không được hỗ trợ");
        }
        ChatMessage message = messages.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn " + messageId));
        if (message.getDeletedAt() != null) {
            throw new BusinessRuleException("Không thể thả cảm xúc vào tin nhắn đã xóa");
        }
        reactions.findByMessageIdAndMemberIdAndEmoji(messageId, actor.getId(), emoji)
                .ifPresentOrElse(reactions::delete, () -> {
                    ChatReaction reaction = new ChatReaction();
                    reaction.setMessage(message);
                    reaction.setMember(actor);
                    reaction.setEmoji(emoji);
                    reactions.save(reaction);
                });
        reactions.flush();
        messages.flush();
        ChatMessage refreshed = messages.findById(messageId).orElseThrow();
        return publish(refreshed);
    }

    private void notifyMentions(ChatMessage message) {
        String normalized = message.getContent().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return;
        members.list(false).stream()
                .filter(member -> !member.getId().equals(message.getSender().getId()))
                .filter(member -> normalized.contains("@" + member.getDisplayName().toLowerCase(Locale.ROOT)))
                .forEach(member -> notificationService.createMention(member, message.getSender(), message.getContent()));
    }

    private ChatMessage findReply(Long messageId) {
        if (messageId == null) return null;
        return messages.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn được trả lời " + messageId));
    }

    private void deleteMediaAfterCommit(List<String> storageKeys) {
        if (storageKeys.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageKeys.forEach(mediaStorage::deleteQuietly);
            }
        });
    }

    private ChatMessageResponse publish(ChatMessage message) {
        ChatMessageResponse response = ApiMapper.chatMessage(message);
        messaging.convertAndSend("/topic/chat", response);
        return response;
    }
}
