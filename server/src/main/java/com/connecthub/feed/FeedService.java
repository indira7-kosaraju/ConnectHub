package com.connecthub.feed;

import com.connecthub.common.CursorPage;
import com.connecthub.common.PageCursor;
import com.connecthub.post.Post;
import com.connecthub.post.PostAssembler;
import com.connecthub.post.PostRepository;
import com.connecthub.post.dto.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Home feed: posts from the user's circles ranked by recency boosted by engagement (see
 * {@link PostRepository#findFeedPage}). The cursor is {@code <rankTime>,<id>} of the last item.
 * Because engagement keeps changing, a post can occasionally move between pages; clients de-dupe by id.
 */
@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository posts;
    private final PostAssembler assembler;
    private final com.connecthub.moderation.BlockService blocks;

    @Transactional(readOnly = true)
    public CursorPage<PostDto> feed(UUID userId, String cursor, Integer limit) {
        PageCursor c = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        List<Object[]> rows = posts.findFeedPage(userId, blocks.hiddenForQuery(userId), c.createdAt(), c.id(), n + 1);
        boolean more = rows.size() > n;
        List<Object[]> pageRows = more ? rows.subList(0, n) : rows;

        List<UUID> ids = pageRows.stream().map(r -> (UUID) r[0]).toList();
        Map<UUID, Post> byId = posts.findAllByIdIn(ids).stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

        String next = null;
        if (more) {
            Object[] last = pageRows.get(n - 1);
            next = new PageCursor(toInstant(last[1]), (UUID) last[0]).encode();
        }
        return new CursorPage<>(assembler.toDtos(ordered, userId), next);
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof OffsetDateTime o) return o.toInstant();
        if (value instanceof Timestamp t) return t.toInstant();
        throw new IllegalStateException("Unexpected timestamp type " + value.getClass());
    }
}
