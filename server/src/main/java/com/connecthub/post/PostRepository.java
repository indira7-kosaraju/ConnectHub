package com.connecthub.post;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @EntityGraph(attributePaths = {"author", "circle", "circle.category"})
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"author", "circle"})
    List<Post> findAllByIdIn(Collection<UUID> ids);

    @Modifying
    @Query("update Post p set p.reactionCount = p.reactionCount + :delta where p.id = :id")
    void addReactions(UUID id, int delta);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + :delta where p.id = :id")
    void addComments(UUID id, int delta);

    /** Circle timeline, newest first, keyset on (created_at, id). */
    @EntityGraph(attributePaths = {"author", "circle"})
    @Query("""
            select p from Post p where p.circle.id = :circleId and p.author.id not in :hidden
              and (p.createdAt < :ts or (p.createdAt = :ts and p.id < :id))
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findCirclePage(UUID circleId, Collection<UUID> hidden, Instant ts, UUID id, Limit limit);

    /** An author's posts in circles the viewer may read (public circles, or ones the viewer is in). */
    @EntityGraph(attributePaths = {"author", "circle"})
    @Query("""
            select p from Post p where p.author.id = :authorId
              and (p.circle.privateCircle = false or p.circle.id in :viewerCircleIds)
              and (p.createdAt < :ts or (p.createdAt = :ts and p.id < :id))
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findAuthorPage(UUID authorId, Collection<UUID> viewerCircleIds, Instant ts, UUID id, Limit limit);

    /**
     * Home feed ranking. Each post gets a "rank time": its creation time pushed forward by
     * engagement — {@code 6h × ln(1 + reactions + 2 × comments)}, capped at 24h — so a lively post
     * from this morning can outrank a silent one from an hour ago, but nothing stays on top forever.
     * Keyset pagination on (rank_at, id). Rows: [id UUID, rank_at timestamptz].
     */
    @Query(value = """
            SELECT r.id, r.rank_at FROM (
                SELECT p.id,
                       p.created_at + make_interval(secs => LEAST(86400,
                           21600 * ln(1 + p.reaction_count + 2 * p.comment_count))) AS rank_at
                FROM posts p
                WHERE p.circle_id IN (
                        SELECT m.circle_id FROM circle_members m
                        WHERE m.user_id = :userId AND m.status = 'active')
                  AND p.created_at < :cursorTs
                  AND p.author_id NOT IN (:hidden)
            ) r
            WHERE (r.rank_at, r.id) < (:cursorTs, :cursorId)
            ORDER BY r.rank_at DESC, r.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findFeedPage(UUID userId, Collection<UUID> hidden, Instant cursorTs, UUID cursorId, int limit);
}
