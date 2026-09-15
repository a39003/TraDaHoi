package com.trasua.service;

import com.trasua.api.ApiMapper;
import com.trasua.api.dto.AuthRequests;
import com.trasua.api.dto.AuthResponse;
import com.trasua.api.dto.ResetCodeResponse;
import com.trasua.domain.AuthSession;
import com.trasua.domain.Member;
import com.trasua.repository.AuthSessionRepository;
import com.trasua.repository.ChatMessageRepository;
import com.trasua.repository.MemberRepository;
import com.trasua.support.BusinessRuleException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;

@Service
public class AuthService {
    private final MemberRepository members;
    private final AuthSessionRepository sessions;
    private final ChatMessageRepository chatMessages;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(MemberRepository members, AuthSessionRepository sessions, ChatMessageRepository chatMessages) {
        this.members = members;
        this.sessions = sessions;
        this.chatMessages = chatMessages;
    }

    @Transactional
    public AuthResponse register(AuthRequests.Register request) {
        String email = email(request.email());
        if (members.existsByEmailIgnoreCase(email)) throw new BusinessRuleException("Email đã được sử dụng");
        Member member = new Member();
        member.setDisplayName(request.displayName().trim());
        member.setEmail(email);
        member.setPasswordHash(passwords.encode(request.password()));
        member.setRole("MEMBER");
        member.setLastChatReadMessageId(chatMessages.findMaxId());
        members.save(member);
        return createSession(member);
    }

    @Transactional
    public AuthResponse login(AuthRequests.Login request) {
        Member member = members.findByEmailIgnoreCase(email(request.email()))
                .orElseThrow(() -> new BusinessRuleException("Email hoặc mật khẩu không đúng"));
        if (!member.isActive() || member.getPasswordHash() == null || !passwords.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessRuleException("Email hoặc mật khẩu không đúng");
        }
        return createSession(member);
    }

    @Transactional
    public ResetCodeResponse requestReset(AuthRequests.RequestReset request) {
        Member member = members.findByEmailIgnoreCase(email(request.email()))
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy tài khoản với email này"));
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        member.setResetCodeHash(sha256(code));
        member.setResetCodeExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        return new ResetCodeResponse("Mã đặt lại có hiệu lực trong 15 phút", code);
    }

    @Transactional
    public void resetPassword(AuthRequests.ResetPassword request) {
        Member member = members.findByEmailIgnoreCase(email(request.email()))
                .orElseThrow(() -> new BusinessRuleException("Mã đặt lại không hợp lệ"));
        if (member.getResetCodeHash() == null || member.getResetCodeExpiresAt() == null
                || member.getResetCodeExpiresAt().isBefore(Instant.now())
                || !member.getResetCodeHash().equals(sha256(request.code()))) {
            throw new BusinessRuleException("Mã đặt lại không hợp lệ hoặc đã hết hạn");
        }
        member.setPasswordHash(passwords.encode(request.password()));
        member.setResetCodeHash(null);
        member.setResetCodeExpiresAt(null);
    }

    private AuthResponse createSession(Member member) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthSession session = new AuthSession();
        session.setMember(member);
        session.setTokenHash(sha256(token));
        session.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        sessions.save(session);
        return new AuthResponse(token, ApiMapper.member(member));
    }

    @Transactional(readOnly = true)
    public void requireAdmin(String authorization) {
        Member member = currentMember(authorization);
        if (!"ADMIN".equals(member.getRole())) {
            throw new BusinessRuleException("Bạn không có quyền quản trị");
        }
    }

    @Transactional(readOnly = true)
    public Member currentMember(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessRuleException("Bạn cần đăng nhập để thực hiện thao tác này");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        AuthSession session = sessions.findByTokenHash(sha256(token))
                .orElseThrow(() -> new BusinessRuleException("Phiên đăng nhập không hợp lệ"));
        if (session.getExpiresAt().isBefore(Instant.now()) || !session.getMember().isActive()) {
            throw new BusinessRuleException("Phiên đăng nhập đã hết hạn hoặc tài khoản đã bị khóa");
        }
        return session.getMember();
    }

    private String email(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
