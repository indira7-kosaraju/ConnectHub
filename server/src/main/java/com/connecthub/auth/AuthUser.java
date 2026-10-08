package com.connecthub.auth;

import java.security.Principal;
import java.util.UUID;

/**
 * Principal placed in the SecurityContext by {@link JwtAuthFilter} (and on STOMP sessions by the
 * WebSocket channel interceptor). Inject with {@code @AuthenticationPrincipal}. Its name is the
 * user id, which is what Spring's user destinations ({@code /user/{name}/queue/…}) route on.
 */
public record AuthUser(UUID id) implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }
}
