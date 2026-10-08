package com.connecthub.notification;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @EntityGraph(attributePaths = {"actor", "post", "comment", "circle"})
    @Query("""
            select n from Notification n where n.recipient.id = :userId
              and (n.createdAt < :ts or (n.createdAt = :ts and n.id < :id))
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findPage(UUID userId, Instant ts, UUID id, Limit limit);

    @EntityGraph(attributePaths = {"actor", "post", "comment", "circle"})
    @Query("select n from Notification n where n.id = :id")
    Optional<Notification> findDetailedById(UUID id);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    /** An unread notification of the same kind from the same person about the same thing (for de-duplication). */
    @Query("""
            select n from Notification n where n.recipient.id = :recipientId and n.actor.id = :actorId
              and n.type = :type and n.read = false
              and ((:postId is null and n.post is null) or n.post.id = :postId)
            """)
    List<Notification> findUnreadDuplicate(UUID recipientId, UUID actorId, NotificationType type, UUID postId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient.id = :userId and n.read = false")
    int markAllRead(UUID userId);
}
