package com.trasua.service;

import com.trasua.api.dto.MemberRequest;
import com.trasua.api.dto.AdminCreateMemberRequest;
import com.trasua.domain.Member;
import com.trasua.repository.ChatAttachmentRepository;
import com.trasua.repository.ChatMessageRepository;
import com.trasua.repository.MemberRepository;
import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class MemberService {
    private final MemberRepository members;
    private final ChatMessageRepository chatMessages;
    private final ChatAttachmentRepository chatAttachments;
    private final ChatMediaStorageService mediaStorage;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public MemberService(MemberRepository members, ChatMessageRepository chatMessages,
                         ChatAttachmentRepository chatAttachments, ChatMediaStorageService mediaStorage,
                         JdbcTemplate jdbc) {
        this.members = members;
        this.chatMessages = chatMessages;
        this.chatAttachments = chatAttachments;
        this.mediaStorage = mediaStorage;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Member> list(boolean includeInactive) {
        return includeInactive ? members.findAll() : members.findByActiveTrueOrderByDisplayNameAsc();
    }

    @Transactional(readOnly = true)
    public Member getRequired(Long id) {
        return members.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên " + id));
    }

    @Transactional
    public Member create(MemberRequest request) {
        ensureEmailAvailable(request.email(), null);
        Member member = new Member();
        apply(member, request);
        return members.save(member);
    }

    @Transactional
    public Member createAccount(AdminCreateMemberRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        ensureEmailAvailable(normalizedEmail, null);
        Member member = new Member();
        member.setDisplayName(request.displayName().trim());
        member.setEmail(normalizedEmail);
        member.setPasswordHash(passwords.encode(request.password()));
        member.setRole(request.role() == null || request.role().isBlank() ? "MEMBER" : request.role());
        member.setActive(true);
        member.setLastChatReadMessageId(chatMessages.findMaxId());
        return members.save(member);
    }

    @Transactional
    public Member update(Long id, MemberRequest request) {
        Member member = getRequired(id);
        ensureEmailAvailable(request.email(), id);
        apply(member, request);
        return member;
    }

    @Transactional
    public void deletePermanently(Long id, Long actorId) {
        if (id.equals(actorId)) {
            throw new BusinessRuleException("Bạn không thể xóa tài khoản đang đăng nhập");
        }
        Member member = getRequired(id);
        List<String> storedKeys = new ArrayList<>(chatAttachments.findStorageKeysBySenderId(id));
        String avatarStorageKey = avatarStorageKey(member.getAvatarUrl());
        if (avatarStorageKey != null) storedKeys.add(avatarStorageKey);
        // Xóa vật lý theo thứ tự để các khóa ngoại không biến thao tác "Xóa"
        // thành khóa/ẩn tài khoản hoặc làm giao dịch bị rollback.
        jdbc.update("DELETE FROM notifications WHERE recipient_member_id = ?", id);
        jdbc.update("DELETE FROM settlement_transfers WHERE debtor_member_id = ? OR creditor_member_id = ?", id, id);
        jdbc.update("DELETE FROM settlement_balances WHERE member_id = ?", id);
        jdbc.update("DELETE FROM chat_messages WHERE sender_member_id = ?", id);
        jdbc.update("DELETE FROM order_items WHERE consumer_member_id = ?", id);
        jdbc.update("DELETE FROM auth_sessions WHERE member_id = ?", id);
        jdbc.update("DELETE FROM drink_orders WHERE payer_member_id = ? OR created_by_member_id = ?", id, id);
        int deleted = jdbc.update("DELETE FROM members WHERE id = ?", id);
        if (deleted != 1) {
            throw new ResourceNotFoundException("Không tìm thấy thành viên " + id);
        }
        if (!storedKeys.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storedKeys.forEach(mediaStorage::deleteQuietly);
                }
            });
        }
    }

    public Member requireActive(Long id) {
        Member member = getRequired(id);
        if (!member.isActive()) {
            throw new BusinessRuleException("Thành viên " + member.getDisplayName() + " đang bị tắt");
        }
        return member;
    }
    @Transactional
    public Member updateAvatar(Long id, String avatarUrl) {
        Member member = getRequired(id);
        member.setAvatarUrl(avatarUrl);
        return member;
    }

    @Transactional
    public void markChatRead(Long memberId, Long messageId) {
        Member member = getRequired(memberId);
        member.setLastChatReadMessageId(messageId);
    }

    private void ensureEmailAvailable(String email, Long id) {
        if (email == null || email.isBlank()) {
            return;
        }
        boolean used = id == null ? members.existsByEmailIgnoreCase(email) : members.existsByEmailIgnoreCaseAndIdNot(email, id);
        if (used) {
            throw new BusinessRuleException("Email này đã được sử dụng");
        }
    }

    private void apply(Member member, MemberRequest request) {
        member.setDisplayName(request.displayName().trim());
        member.setEmail(blankToNull(request.email()));
        member.setBankName(blankToNull(request.bankName()));
        member.setBankBin(blankToNull(request.bankBin()));
        member.setAccountNumber(blankToNull(request.accountNumber()));
        member.setAccountName(blankToNull(request.accountName()));
        member.setQrCodeUrl(blankToNull(request.qrCodeUrl()));
        member.setAvatarUrl(blankToNull(request.avatarUrl()));
        if (request.active() != null) {
            member.setActive(request.active());
        }
        if (request.role() != null && !request.role().isBlank()) {
            member.setRole(request.role());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String avatarStorageKey(String avatarUrl) {
        String prefix = "/api/members/avatar/";
        if (avatarUrl == null || !avatarUrl.startsWith(prefix)) return null;
        String storageKey = avatarUrl.substring(prefix.length());
        return storageKey.isBlank() ? null : storageKey;
    }
}
