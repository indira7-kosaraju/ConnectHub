package com.connecthub.message;

import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Creates a conversation in its own transaction so a unique-key race can be retried by the caller. */
@Component
@RequiredArgsConstructor
class ConversationCreator {

    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final UserRepository users;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Conversation create(UUID a, UUID b) {
        Conversation c = new Conversation();
        c.setDirectKey(Conversation.directKey(a, b));
        conversations.saveAndFlush(c);
        participants.save(new ConversationParticipant(c, users.getReferenceById(a)));
        participants.save(new ConversationParticipant(c, users.getReferenceById(b)));
        return c;
    }
}
