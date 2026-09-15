package com.trasua.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_message_reactions")
public class ChatReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 32)
    private String emoji;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void created() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public ChatMessage getMessage() { return message; }
    public void setMessage(ChatMessage message) { this.message = message; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public Instant getCreatedAt() { return createdAt; }
}
