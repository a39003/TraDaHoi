package com.trasua.repository;

import com.trasua.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            select n from Notification n
            join fetch n.recipient
            where n.recipient.id = :memberId
            order by n.createdAt desc
            """)
    List<Notification> findDetailedByRecipientId(@Param("memberId") Long memberId);

    @Query("""
            select n from Notification n
            join fetch n.recipient
            where n.id = :id and n.recipient.id = :memberId
            """)
    Optional<Notification> findOwnedById(@Param("id") Long id, @Param("memberId") Long memberId);
}
