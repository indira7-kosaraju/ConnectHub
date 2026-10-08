package com.connecthub.message;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** Newest first (the client renders bottom-up), keyset on (created_at, id). */
    @EntityGraph(attributePaths = {"sender", "sharedPost"})
    @Query("""
            select m from Message m where m.conversation.id = :conversationId
              and (m.createdAt < :ts or (m.createdAt = :ts and m.id < :id))
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findPage(UUID conversationId, Instant ts, UUID id, Limit limit);

    /** Latest message of each conversation. */
    @Query(value = """
            SELECT DISTINCT ON (conversation_id) id FROM messages
            WHERE conversation_id IN (:conversationIds)
            ORDER BY conversation_id, created_at DESC, id DESC
            """, nativeQuery = true)
    List<UUID> findLatestIds(Collection<UUID> conversationIds);

    @EntityGraph(attributePaths = {"sender", "sharedPost"})
    List<Message> findAllByIdIn(Collection<UUID> ids);

    /** Rows: [conversationId, unreadCount] for messages sent to {@code userId}. */
    @Query("""
            select m.conversation.id, count(m) from Message m
            where m.conversation.id in :conversationIds and m.sender.id <> :userId and m.readAt is null
            group by m.conversation.id
            """)
    List<Object[]> countUnread(Collection<UUID> conversationIds, UUID userId);

    /** Number of conversations with at least one unread message for the user (the Messages badge). */
    @Query("""
            select count(distinct m.conversation.id) from Message m, ConversationParticipant p
            where p.conversation = m.conversation and p.user.id = :userId
              and m.sender.id <> :userId and m.readAt is null
            """)
    long countUnreadConversations(UUID userId);

    @Modifying
    @Query("""
            update Message m set m.readAt = :now
            where m.conversation.id = :conversationId and m.sender.id <> :readerId and m.readAt is null
            """)
    int markRead(UUID conversationId, UUID readerId, Instant now);
}
