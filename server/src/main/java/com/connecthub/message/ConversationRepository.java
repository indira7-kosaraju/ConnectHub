package com.connecthub.message;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByDirectKey(String directKey);

    /** The user's conversations that have at least one message, most recent first. */
    @Query("""
            select c from Conversation c, ConversationParticipant p
            where p.conversation = c and p.user.id = :userId and c.lastMessageAt is not null
              and (c.lastMessageAt < :ts or (c.lastMessageAt = :ts and c.id < :id))
            order by c.lastMessageAt desc, c.id desc
            """)
    List<Conversation> findPageForUser(UUID userId, Instant ts, UUID id, Limit limit);
}
