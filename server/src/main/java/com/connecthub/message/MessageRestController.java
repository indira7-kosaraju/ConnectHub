package com.connecthub.message;

import com.connecthub.auth.AuthUser;
import com.connecthub.common.ApiResponse;
import com.connecthub.common.CursorPage;
import com.connecthub.common.RateLimiter;
import com.connecthub.message.dto.ConversationDto;
import com.connecthub.message.dto.MessageDto;
import com.connecthub.message.dto.ReadReceipt;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.dto.StartConversationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** REST side of messaging: history, conversation list, and a send fallback when the socket is down. */
@Tag(name = "Messages")
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class MessageRestController {

    private final MessageService messages;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Your conversations, most recent first (only ones with messages)")
    @GetMapping
    public ApiResponse<CursorPage<ConversationDto>> list(@AuthenticationPrincipal AuthUser me,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(messages.list(me.id(), cursor, limit));
    }

    @Operation(summary = "Open (find or create) a 1:1 conversation with someone who shares a circle with you")
    @PostMapping
    public ApiResponse<ConversationDto> start(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody StartConversationRequest req) {
        return ApiResponse.ok(messages.start(me.id(), req.userId()));
    }

    @Operation(summary = "Number of conversations with unread messages (the Messages badge)")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unread(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(Map.of("count", messages.unreadConversations(me.id())));
    }

    @Operation(summary = "One conversation")
    @GetMapping("/{id}")
    public ApiResponse<ConversationDto> get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(messages.get(me.id(), id));
    }

    @Operation(summary = "Messages, newest first; cursor = <createdAt>,<id>")
    @GetMapping("/{id}/messages")
    public ApiResponse<CursorPage<MessageDto>> history(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                                       @RequestParam(required = false) String cursor,
                                                       @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(messages.messages(me.id(), id, cursor, limit));
    }

    @Operation(summary = "Send a message (REST fallback; the realtime path is STOMP /app/chat.send). 60 / min.")
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageDto> send(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                        @Valid @RequestBody SendMessageRequest req) {
        rateLimiter.consume(RateLimiter.Limit.MESSAGE, me.id().toString());
        return ApiResponse.ok(messages.send(me.id(), id, req));
    }

    @Operation(summary = "Mark the conversation read (sends a read receipt to the other person)")
    @PostMapping("/{id}/read")
    public ApiResponse<ReadReceipt> read(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(messages.markRead(me.id(), id));
    }
}
