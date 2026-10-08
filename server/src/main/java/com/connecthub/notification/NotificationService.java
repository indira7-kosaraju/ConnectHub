package com.connecthub.notification;

import com.connecthub.circle.Circle;
import com.connecthub.circle.CircleMapper;
import com.connecthub.comment.Comment;
import com.connecthub.common.CursorPage;
import com.connecthub.common.PageCursor;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.common.realtime.RealtimePublisher;
import com.connecthub.notification.dto.NotificationDto;
import com.connecthub.post.Post;
import com.connecthub.post.ReactionType;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int SNIPPET = 80;

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EntityManager em;
    private final CircleMapper mapper;
    private final RealtimePublisher realtime;
    private final com.connecthub.post.ReactionRepository reactions;
    private final com.connecthub.moderation.BlockService blocks;

    /** What a notification is about. All fields optional. */
    public record Subject(UUID postId, UUID commentId, UUID circleId, ReactionType reaction) {
        public static Subject post(UUID postId) {
            return new Subject(postId, null, null, null);
        }
    }

    /**
     * Creates a notification (in its own transaction, so it commits even when called after the
     * triggering transaction) and pushes it to the recipient. Skips self-notifications, respects the
     * recipient's preferences, and folds repeats: a second unread reaction or message from the same
     * person refreshes the existing notification instead of stacking.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(UUID recipientId, UUID actorId, NotificationType type, Subject subject) {
        if (recipientId == null || recipientId.equals(actorId) || blocks.blockedEitherWay(recipientId, actorId)) {
            return;
        }
        User recipient = users.findById(recipientId).orElse(null);
        if (recipient == null || !Boolean.TRUE.equals(recipient.getNotificationPrefs().getOrDefault(type.prefKey(), true))) {
            return;
        }
        if ((type == NotificationType.reaction || type == NotificationType.message) && actorId != null) {
            UUID postKey = type == NotificationType.reaction ? subject.postId() : null;
            List<Notification> dupes = notifications.findUnreadDuplicate(recipientId, actorId, type, postKey);
            if (!dupes.isEmpty()) {
                Notification n = dupes.get(0);
                n.setCreatedAt(Instant.now()); // bubble it back to the top
                realtime.toUser(recipientId, RealtimePublisher.NOTIFICATIONS, toDto(n, subject.reaction()));
                return;
            }
        }
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setActor(actorId == null ? null : users.getReferenceById(actorId));
        n.setType(type);
        if (subject.postId() != null) n.setPost(em.getReference(Post.class, subject.postId()));
        if (subject.commentId() != null) n.setComment(em.getReference(Comment.class, subject.commentId()));
        if (subject.circleId() != null) n.setCircle(em.getReference(Circle.class, subject.circleId()));
        notifications.saveAndFlush(n);
        Notification loaded = notifications.findDetailedById(n.getId()).orElseThrow();
        realtime.toUser(recipientId, RealtimePublisher.NOTIFICATIONS, toDto(loaded, subject.reaction()));
    }

    @Transactional(readOnly = true)
    public CursorPage<NotificationDto> list(UUID userId, String cursor, Integer limit) {
        PageCursor pc = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        List<Notification> rows = notifications.findPage(userId, pc.createdAt(), pc.id(), Limit.of(n + 1));
        boolean more = rows.size() > n;
        List<Notification> page = more ? rows.subList(0, n) : rows;
        String next = more ? new PageCursor(page.get(n - 1).getCreatedAt(), page.get(n - 1).getId()).encode() : null;
        // Reaction notifications show the actor's current vibe (one query for the page).
        java.util.Map<String, ReactionType> vibes = new java.util.HashMap<>();
        var reactionRows = page.stream().filter(x -> x.getType() == NotificationType.reaction && x.getPost() != null && x.getActor() != null).toList();
        if (!reactionRows.isEmpty()) {
            for (Object[] r : reactions.findTypes(reactionRows.stream().map(x -> x.getPost().getId()).toList(),
                    reactionRows.stream().map(x -> x.getActor().getId()).toList())) {
                vibes.put(r[0] + ":" + r[1], (ReactionType) r[2]);
            }
        }
        return new CursorPage<>(page.stream().map(x -> toDto(x, x.getType() == NotificationType.reaction && x.getPost() != null && x.getActor() != null
                ? vibes.get(x.getPost().getId() + ":" + x.getActor().getId()) : null)).toList(), next);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification n = notifications.findById(id)
                .filter(x -> x.getRecipient().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        n.setRead(true);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notifications.markAllRead(userId);
    }

    private NotificationDto toDto(Notification n, ReactionType reaction) {
        Post post = n.getPost();
        Comment comment = n.getComment();
        Circle circle = n.getCircle();
        return new NotificationDto(n.getId(), n.getType(),
                n.getActor() == null ? null : mapper.toUserSummary(n.getActor()),
                post == null ? null : post.getId(), post == null ? null : snippet(post.getContent()),
                comment == null ? null : comment.getId(), comment == null ? null : snippet(comment.getContent()),
                circle == null ? null : circle.getSlug(), circle == null ? null : circle.getName(),
                reaction, n.isRead(), n.getCreatedAt());
    }

    private static String snippet(String text) {
        if (text == null) return null;
        return text.length() <= SNIPPET ? text : text.substring(0, SNIPPET - 1).stripTrailing() + "…";
    }
}
