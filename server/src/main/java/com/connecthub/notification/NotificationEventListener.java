package com.connecthub.notification;

import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.circle.CircleRole;
import com.connecthub.circle.MemberStatus;
import com.connecthub.circle.event.CircleJoinApprovedEvent;
import com.connecthub.circle.event.CircleJoinRequestedEvent;
import com.connecthub.comment.event.CommentCreatedEvent;
import com.connecthub.message.MessageSentEvent;
import com.connecthub.notification.NotificationService.Subject;
import com.connecthub.post.event.PostReactedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

/**
 * Turns domain events into notifications <b>after</b> the triggering transaction commits, so a
 * rolled-back action never notifies anyone and a notification failure never breaks the action.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notifications;
    private final CircleMemberRepository members;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReaction(PostReactedEvent e) {
        safely(() -> notifications.notify(e.postAuthorId(), e.actorId(), NotificationType.reaction,
                new Subject(e.postId(), null, null, e.type())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComment(CommentCreatedEvent e) {
        safely(() -> {
            if (e.parentAuthorId() != null) {
                notifications.notify(e.parentAuthorId(), e.authorId(), NotificationType.reply,
                        new Subject(e.postId(), e.commentId(), null, null));
            }
            // The post's author hears about every comment — unless they were just notified as the parent author.
            if (!Objects.equals(e.postAuthorId(), e.parentAuthorId())) {
                notifications.notify(e.postAuthorId(), e.authorId(), NotificationType.comment,
                        new Subject(e.postId(), e.commentId(), null, null));
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJoinRequest(CircleJoinRequestedEvent e) {
        safely(() -> members.findByCircleIdAndRoleAndStatusOrderByCreatedAtAsc(e.circleId(), CircleRole.admin, MemberStatus.active)
                .forEach(admin -> notifications.notify(admin.getUser().getId(), e.requesterId(), NotificationType.circle_request,
                        new Subject(null, null, e.circleId(), null))));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJoinApproved(CircleJoinApprovedEvent e) {
        safely(() -> notifications.notify(e.userId(), e.approvedBy(), NotificationType.circle_approved,
                new Subject(null, null, e.circleId(), null)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessage(MessageSentEvent e) {
        safely(() -> notifications.notify(e.recipientId(), e.senderId(), NotificationType.message,
                new Subject(null, null, null, null)));
    }

    private static void safely(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ex) {
            log.error("Failed to create notification", ex);
        }
    }
}
