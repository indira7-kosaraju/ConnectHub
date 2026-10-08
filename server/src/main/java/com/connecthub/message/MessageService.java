package com.connecthub.message;

import com.connecthub.circle.CircleMapper;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.common.CursorPage;
import com.connecthub.common.PageCursor;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.common.realtime.RealtimePublisher;
import com.connecthub.message.dto.ConversationDto;
import com.connecthub.message.dto.MessageDto;
import com.connecthub.message.dto.ReadReceipt;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.dto.TypingEvent;
import com.connecthub.post.Post;
import com.connecthub.post.PostAccess;
import com.connecthub.post.PostAssembler;
import com.connecthub.post.PostRepository;
import com.connecthub.post.dto.PostDto;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 1:1 direct messages. New conversations can only be started with people who share a circle with you. */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final MessageRepository messages;
    private final UserRepository users;
    private final CircleMemberRepository members;
    private final PostRepository posts;
    private final PostAccess postAccess;
    private final PostAssembler postAssembler;
    private final CircleMapper mapper;
    private final RealtimePublisher realtime;
    private final ApplicationEventPublisher events;
    private final ConversationCreator creator;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.moderation.ProfanityFilter profanity;

    // ------------------------------------------------------------------ conversations

    /** Find or create the 1:1 conversation with {@code otherId}. */
    @Transactional
    public ConversationDto start(UUID meId, UUID otherId) {
        if (meId.equals(otherId)) {
            throw new BadRequestException("You can't message yourself");
        }
        User other = users.findById(otherId).filter(u -> u.getUsername() != null)
                .filter(u -> !blocks.blockedEitherWay(meId, u.getId()))
                .orElseThrow(() -> new NotFoundException("User not found"));
        Conversation c = conversations.findByDirectKey(Conversation.directKey(meId, otherId)).orElse(null);
        if (c == null) {
            if (!members.shareActiveCircle(meId, otherId)) {
                throw new ForbiddenException("You can message people who share a circle with you");
            }
            c = createOrGet(meId, other);
        }
        return toConversationDtos(List.of(c), meId).get(0);
    }

    @Transactional(readOnly = true)
    public CursorPage<ConversationDto> list(UUID meId, String cursor, Integer limit) {
        PageCursor pc = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        List<Conversation> rows = conversations.findPageForUser(meId, pc.createdAt(), pc.id(), Limit.of(n + 1));
        boolean more = rows.size() > n;
        List<Conversation> page = more ? rows.subList(0, n) : rows;
        String next = more ? new PageCursor(page.get(n - 1).getLastMessageAt(), page.get(n - 1).getId()).encode() : null;
        java.util.Set<UUID> hidden = blocks.hiddenFrom(meId);
        return new CursorPage<>(toConversationDtos(page, meId).stream().filter(c -> !hidden.contains(c.other().id())).toList(), next);
    }

    @Transactional(readOnly = true)
    public ConversationDto get(UUID meId, UUID conversationId) {
        return toConversationDtos(List.of(requireParticipant(conversationId, meId).getConversation()), meId).get(0);
    }

    @Transactional(readOnly = true)
    public long unreadConversations(UUID meId) {
        return messages.countUnreadConversations(meId);
    }

    // ------------------------------------------------------------------ messages

    @Transactional(readOnly = true)
    public CursorPage<MessageDto> messages(UUID meId, UUID conversationId, String cursor, Integer limit) {
        requireParticipant(conversationId, meId);
        PageCursor pc = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        List<Message> rows = messages.findPage(conversationId, pc.createdAt(), pc.id(), Limit.of(n + 1));
        boolean more = rows.size() > n;
        List<Message> page = more ? rows.subList(0, n) : rows;
        String next = more ? new PageCursor(page.get(n - 1).getCreatedAt(), page.get(n - 1).getId()).encode() : null;
        return new CursorPage<>(toMessageDtos(page, meId, null), next);
    }

    /** Persists a message and pushes it (after commit) to both participants. */
    @Transactional
    public MessageDto send(UUID senderId, UUID conversationId, SendMessageRequest req) {
        ConversationParticipant me = requireParticipant(conversationId, senderId);
        Conversation conversation = me.getConversation();
        String content = profanity.clean(TextSanitizer.plain(req.content()));
        Post shared = null;
        if (req.sharedPostId() != null) {
            shared = posts.findDetailedById(req.sharedPostId()).orElseThrow(() -> new NotFoundException("Post not found"));
            postAccess.requireReadable(shared, senderId);
        }
        if (content == null && shared == null) {
            throw new BadRequestException("Message is empty");
        }
        UUID recipientId = participants.findOthers(List.of(conversationId), senderId).stream()
                .map(p -> p.getUser().getId()).findFirst()
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        if (blocks.blockedEitherWay(senderId, recipientId)) {
            throw new ForbiddenException("You can't message this person");
        }

        Message m = new Message();
        m.setConversation(conversation);
        m.setSender(users.getReferenceById(senderId));
        m.setContent(content);
        m.setSharedPost(shared);
        messages.saveAndFlush(m);
        conversation.setLastMessageAt(m.getCreatedAt());
        me.setLastReadAt(m.getCreatedAt()); // sending implies you've read up to here

        MessageDto forSender = toMessageDtos(List.of(m), senderId, req.clientId()).get(0);
        MessageDto forRecipient = toMessageDtos(List.of(m), recipientId, null).get(0);
        realtime.toUser(senderId, RealtimePublisher.MESSAGES, forSender);
        realtime.toUser(recipientId, RealtimePublisher.MESSAGES, forRecipient);
        events.publishEvent(new MessageSentEvent(conversationId, senderId, recipientId));
        return forSender;
    }

    /** Marks everything the other person sent as read and sends them a read receipt. */
    @Transactional
    public ReadReceipt markRead(UUID meId, UUID conversationId) {
        ConversationParticipant me = requireParticipant(conversationId, meId);
        Instant now = Instant.now();
        int updated = messages.markRead(conversationId, meId, now);
        me.setLastReadAt(now);
        ReadReceipt receipt = new ReadReceipt(conversationId, meId, now);
        if (updated > 0) {
            participants.findOthers(List.of(conversationId), meId)
                    .forEach(p -> realtime.toUser(p.getUser().getId(), RealtimePublisher.READ, receipt));
        }
        return receipt;
    }

    /** Relays a typing indicator to the other participant. Not persisted. */
    @Transactional(readOnly = true)
    public void typing(UUID meId, UUID conversationId, boolean typing) {
        requireParticipant(conversationId, meId);
        TypingEvent event = new TypingEvent(conversationId, meId, typing);
        participants.findOthers(List.of(conversationId), meId)
                .forEach(p -> realtime.toUser(p.getUser().getId(), RealtimePublisher.TYPING, event));
    }

    // ------------------------------------------------------------------ helpers

    private Conversation createOrGet(UUID meId, User other) {
        try {
            return creator.create(meId, other.getId());
        } catch (DataIntegrityViolationException race) {
            // Both people started the conversation at the same moment: use the one that won.
            return conversations.findByDirectKey(Conversation.directKey(meId, other.getId())).orElseThrow();
        }
    }

    private ConversationParticipant requireParticipant(UUID conversationId, UUID userId) {
        return participants.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private List<ConversationDto> toConversationDtos(List<Conversation> list, UUID meId) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = list.stream().map(Conversation::getId).toList();
        Map<UUID, ConversationParticipant> others = participants.findOthers(ids, meId).stream()
                .collect(Collectors.toMap(p -> p.getConversation().getId(), Function.identity()));
        Map<UUID, MessageDto> latest = new HashMap<>();
        List<UUID> latestIds = messages.findLatestIds(ids);
        if (!latestIds.isEmpty()) {
            toMessageDtos(messages.findAllByIdIn(latestIds), meId, null).forEach(m -> latest.put(m.conversationId(), m));
        }
        Map<UUID, Long> unread = new HashMap<>();
        for (Object[] row : messages.countUnread(ids, meId)) {
            unread.put((UUID) row[0], (Long) row[1]);
        }
        return list.stream().map(c -> {
            ConversationParticipant other = others.get(c.getId());
            return new ConversationDto(c.getId(), mapper.toUserSummary(other.getUser()), latest.get(c.getId()),
                    unread.getOrDefault(c.getId(), 0L).intValue(), c.getLastMessageAt(), other.getLastReadAt());
        }).toList();
    }

    /** Hydrates messages for one viewer; shared posts the viewer can't read are withheld. */
    private List<MessageDto> toMessageDtos(List<Message> list, UUID viewerId, String clientId) {
        List<Post> shared = list.stream().map(Message::getSharedPost).filter(Objects::nonNull).distinct()
                .filter(p -> canRead(p, viewerId)).toList();
        Map<UUID, PostDto> postDtos = postAssembler.toDtos(shared, viewerId).stream()
                .collect(Collectors.toMap(PostDto::id, Function.identity()));
        return list.stream().map(m -> {
            PostDto post = m.getSharedPost() == null ? null : postDtos.get(m.getSharedPost().getId());
            boolean unavailable = m.getSharedPost() == null ? false : post == null;
            return new MessageDto(m.getId(), m.getConversation().getId(), m.getSender().getId(),
                    mapper.toUserSummary(m.getSender()), m.getContent(), post,
                    unavailable, m.getCreatedAt(), m.getReadAt(), clientId);
        }).toList();
    }

    private boolean canRead(Post post, UUID viewerId) {
        try {
            postAccess.requireReadable(post, viewerId);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }
}
