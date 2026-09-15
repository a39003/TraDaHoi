package com.trasua.repository;

import com.trasua.domain.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;
import java.util.List;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    @Query("select a.storageKey from ChatAttachment a")
    Set<String> findAllStorageKeys();

    @Query("select a.storageKey from ChatAttachment a where a.message.sender.id = :memberId")
    List<String> findStorageKeysBySenderId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
