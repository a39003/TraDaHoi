package com.trasua.repository;

import com.trasua.domain.ChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatReactionRepository extends JpaRepository<ChatReaction, Long> {
    Optional<ChatReaction> findByMessageIdAndMemberIdAndEmoji(Long messageId, Long memberId, String emoji);
}
