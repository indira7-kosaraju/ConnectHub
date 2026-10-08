package com.connecthub.config;

import com.connecthub.auth.AuthUser;
import com.connecthub.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates STOMP sessions. CONNECT must carry {@code Authorization: Bearer <jwt>} as a STOMP
 * header (SockJS can't send HTTP headers). Clients may only subscribe to their own user queues;
 * everything they need — messages, typing, read receipts, notifications — is delivered there.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final List<SimpleGrantedAuthority> USER = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing access token");
            }
            var userId = jwtService.verify(header.substring(7).trim())
                    .orElseThrow(() -> new MessageDeliveryException("Invalid or expired access token"));
            accessor.setUser(new UsernamePasswordAuthenticationToken(new AuthUser(userId), null, USER));
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (accessor.getUser() == null || destination == null || !destination.startsWith("/user/queue/")) {
                throw new MessageDeliveryException("Subscription not allowed");
            }
        } else if (command == StompCommand.SEND && accessor.getUser() == null) {
            throw new MessageDeliveryException("Not authenticated");
        }
        return message;
    }
}
