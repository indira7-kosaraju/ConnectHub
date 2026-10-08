package com.connecthub.message;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.RateLimiter;
import com.connecthub.common.exception.ApiException;
import com.connecthub.common.realtime.RealtimePublisher;
import com.connecthub.message.dto.ChatSendFrame;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.dto.TypingEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP endpoints (prefix /app). Results arrive on the user's queues; failures are reported on
 * /user/queue/errors so the client can roll back optimistic messages.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messages;
    private final RateLimiter rateLimiter;
    private final RealtimePublisher realtime;

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload ChatSendFrame frame, Principal principal) {
        UUID me = userId(principal);
        try {
            rateLimiter.consume(RateLimiter.Limit.MESSAGE, me.toString());
            messages.send(me, frame.conversationId(), new SendMessageRequest(frame.content(), frame.sharedPostId(), frame.clientId()));
        } catch (ApiException e) {
            realtime.toUser(me, RealtimePublisher.ERRORS, ApiResponse.fail(e.code(), e.getMessage(),
                    frame.clientId() == null ? null : Map.of("clientId", frame.clientId())));
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event, Principal principal) {
        try {
            messages.typing(userId(principal), event.conversationId(), event.typing());
        } catch (ApiException e) {
            log.debug("Ignoring typing event: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat.read")
    public void read(@Payload Map<String, UUID> body, Principal principal) {
        try {
            messages.markRead(userId(principal), body.get("conversationId"));
        } catch (ApiException e) {
            log.debug("Ignoring read event: {}", e.getMessage());
        }
    }

    @MessageExceptionHandler
    public void onError(Exception e, Principal principal) {
        log.warn("STOMP handler error: {}", e.getMessage());
        if (principal != null) {
            realtime.toUser(userId(principal), RealtimePublisher.ERRORS, ApiResponse.fail("BAD_REQUEST", "Message rejected"));
        }
    }

    private static UUID userId(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication auth && auth.getPrincipal() instanceof AuthUser u) {
            return u.id();
        }
        return UUID.fromString(principal.getName());
    }
}
