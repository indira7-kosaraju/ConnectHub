package com.connecthub.common.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Pushes payloads to a user's private STOMP queue ({@code /user/queue/…}). When called inside a
 * transaction the push is deferred until commit, so clients never receive data they can't yet read
 * back over REST (and never receive data that was rolled back).
 */
@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    public static final String MESSAGES = "/queue/messages";
    public static final String READ = "/queue/read";
    public static final String TYPING = "/queue/typing";
    public static final String NOTIFICATIONS = "/queue/notifications";
    public static final String ERRORS = "/queue/errors";

    private final SimpMessagingTemplate template;

    public void toUser(UUID userId, String destination, Object payload) {
        Runnable send = () -> template.convertAndSendToUser(userId.toString(), destination, payload);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
