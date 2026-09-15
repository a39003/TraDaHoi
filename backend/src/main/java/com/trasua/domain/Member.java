package com.trasua.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 150, unique = true)
    private String email;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_bin", length = 20)
    private String bankBin;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 150)
    private String accountName;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role = "MEMBER";

    @Column(name = "reset_code_hash", length = 64)
    private String resetCodeHash;

    @Column(name = "reset_code_expires_at")
    private java.time.Instant resetCodeExpiresAt;

    @Column(name = "last_chat_read_message_id")
    private Long lastChatReadMessageId;

    public Member() {
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankBin() { return bankBin; }
    public void setBankBin(String bankBin) { this.bankBin = bankBin; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getResetCodeHash() { return resetCodeHash; }
    public void setResetCodeHash(String resetCodeHash) { this.resetCodeHash = resetCodeHash; }
    public java.time.Instant getResetCodeExpiresAt() { return resetCodeExpiresAt; }
    public void setResetCodeExpiresAt(java.time.Instant resetCodeExpiresAt) { this.resetCodeExpiresAt = resetCodeExpiresAt; }
    public Long getLastChatReadMessageId() { return lastChatReadMessageId; }
    public void setLastChatReadMessageId(Long lastChatReadMessageId) { this.lastChatReadMessageId = lastChatReadMessageId; }
}
