package com.connecthub.message;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    /** The other side of each 1:1 conversation. */
    @EntityGraph(attributePaths = "user")
    @Query("select p from ConversationParticipant p where p.conversation.id in :conversationIds and p.user.id <> :userId")
    List<ConversationParticipant> findOthers(Collection<UUID> conversationIds, UUID userId);
}
