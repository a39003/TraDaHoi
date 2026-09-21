package com.trasua.repository;

import com.trasua.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("select max(m.id) from ChatMessage m")
    Long findMaxId();

    long countBySenderIdNotAndIdGreaterThan(Long senderId, Long messageId);

    @Query("select distinct m from ChatMessage m join fetch m.sender left join fetch m.replyTo r left join fetch r.sender where m.createdAt < :before order by m.createdAt desc")
    List<ChatMessage> findOlderThan(@Param("before") Instant before, Pageable pageable);

    @Query("select distinct m from ChatMessage m join fetch m.sender left join fetch m.replyTo r left join fetch r.sender where m.id > :afterId order by m.id asc")
    List<ChatMessage> findNewerThanId(@Param("afterId") Long afterId, Pageable pageable);

    @Query("select distinct m from ChatMessage m join fetch m.sender left join fetch m.replyTo r left join fetch r.sender order by m.createdAt desc")
    List<ChatMessage> findRecent(Pageable pageable);

    @Query("select distinct m from ChatMessage m join fetch m.sender left join fetch m.replyTo r left join fetch r.sender where m.deletedAt is null and lower(m.content) like lower(concat('%', :query, '%')) order by m.createdAt desc")
    List<ChatMessage> searchRecent(@Param("query") String query, Pageable pageable);

    @Query("select distinct m from ChatMessage m join fetch m.sender left join fetch m.replyTo r left join fetch r.sender where m.deletedAt is null and m.createdAt < :before and lower(m.content) like lower(concat('%', :query, '%')) order by m.createdAt desc")
    List<ChatMessage> searchOlder(@Param("query") String query, @Param("before") Instant before, Pageable pageable);

    @Query("select m.id from ChatMessage m where m.createdAt < :cutoff order by m.id")
    List<Long> findExpiredIds(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("select distinct m from ChatMessage m left join fetch m.attachments where m.id in :ids")
    List<ChatMessage> findWithAttachmentsByIdIn(@Param("ids") List<Long> ids);
}
