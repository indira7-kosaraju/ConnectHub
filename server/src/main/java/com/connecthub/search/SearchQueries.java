package com.connecthub.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Full-text search over the generated {@code search_vector} columns (V5). Returns ranked ids;
 * callers hydrate them. Every query uses prefix matching so results appear while typing.
 */
@Component
@RequiredArgsConstructor
public class SearchQueries {

    private static final int MAX_TERMS = 6;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * "Maya ok!" → "maya:* & ok:*". Only letters and digits survive, so user input can never
     * inject tsquery operators. Returns null when nothing searchable remains.
     */
    public static String toPrefixQuery(String raw) {
        if (raw == null) {
            return null;
        }
        String q = Arrays.stream(raw.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(t -> !t.isBlank())
                .limit(MAX_TERMS)
                .map(t -> t + ":*")
                .collect(Collectors.joining(" & "));
        return q.isEmpty() ? null : q;
    }

    public List<UUID> people(String tsQuery, Collection<UUID> hidden, int limit) {
        return jdbc.queryForList("""
                SELECT u.id FROM users u
                WHERE u.username IS NOT NULL
                  AND u.search_vector @@ to_tsquery('simple', :q)
                  AND u.id NOT IN (:hidden)
                ORDER BY ts_rank(u.search_vector, to_tsquery('simple', :q)) DESC, u.username
                LIMIT :limit
                """, new MapSqlParameterSource("q", tsQuery).addValue("hidden", hidden).addValue("limit", limit), UUID.class);
    }

    /** Circles matching by name (any language config) or description, optionally within a category. */
    public List<UUID> circles(String tsQuery, String categorySlug, int limit, int offset) {
        return jdbc.queryForList("""
                SELECT c.id FROM circles c JOIN interests i ON i.id = c.category_id
                WHERE (c.search_vector @@ to_tsquery('simple', :q) OR c.search_vector @@ to_tsquery('english', :q))
                  AND (:category = '' OR i.slug = :category)
                ORDER BY ts_rank(c.search_vector, to_tsquery('simple', :q))
                       + ts_rank(c.search_vector, to_tsquery('english', :q)) DESC,
                         c.member_count DESC, c.id
                LIMIT :limit OFFSET :offset
                """, circleParams(tsQuery, categorySlug).addValue("limit", limit).addValue("offset", offset), UUID.class);
    }

    public long countCircles(String tsQuery, String categorySlug) {
        Long n = jdbc.queryForObject("""
                SELECT count(*) FROM circles c JOIN interests i ON i.id = c.category_id
                WHERE (c.search_vector @@ to_tsquery('simple', :q) OR c.search_vector @@ to_tsquery('english', :q))
                  AND (:category = '' OR i.slug = :category)
                """, circleParams(tsQuery, categorySlug), Long.class);
        return n == null ? 0 : n;
    }

    /** Posts the viewer may read (public circles or ones they're in), excluding blocked authors. */
    public List<UUID> posts(String tsQuery, UUID viewerId, Collection<UUID> hidden, int limit) {
        return jdbc.queryForList("""
                SELECT p.id FROM posts p JOIN circles c ON c.id = p.circle_id
                WHERE p.search_vector @@ to_tsquery('english', :q)
                  AND (c.is_private = FALSE OR c.id IN (
                        SELECT m.circle_id FROM circle_members m WHERE m.user_id = :viewer AND m.status = 'active'))
                  AND p.author_id NOT IN (:hidden)
                ORDER BY ts_rank(p.search_vector, to_tsquery('english', :q)) DESC, p.created_at DESC
                LIMIT :limit
                """, new MapSqlParameterSource("q", tsQuery).addValue("viewer", viewerId).addValue("hidden", hidden)
                .addValue("limit", limit), UUID.class);
    }

    private static MapSqlParameterSource circleParams(String tsQuery, String categorySlug) {
        return new MapSqlParameterSource("q", tsQuery).addValue("category", categorySlug == null ? "" : categorySlug);
    }
}
