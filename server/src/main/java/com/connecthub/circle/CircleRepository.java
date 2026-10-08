package com.connecthub.circle;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircleRepository extends JpaRepository<Circle, UUID>, JpaSpecificationExecutor<Circle> {

    @EntityGraph(attributePaths = "category")
    Optional<Circle> findBySlug(String slug);

    /** Row lock: membership changes serialise per circle so capacity can't be exceeded. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Circle c where c.slug = :slug")
    Optional<Circle> findBySlugForUpdate(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "category")
    List<Circle> findAllByIdIn(Collection<UUID> ids);

    /**
     * Trending = recent momentum (active joins in the last 7 days, weighted ×3) plus size.
     * Excludes circles the viewer already belongs to (pass a random UUID for anonymous viewers).
     */
    @Query(value = """
            SELECT c.id FROM circles c
            LEFT JOIN (
                SELECT circle_id, count(*) AS n FROM circle_members
                WHERE status = 'active' AND created_at > now() - interval '7 days'
                GROUP BY circle_id
            ) recent ON recent.circle_id = c.id
            WHERE NOT EXISTS (SELECT 1 FROM circle_members m WHERE m.circle_id = c.id AND m.user_id = :viewerId)
            ORDER BY coalesce(recent.n, 0) * 3 + c.member_count DESC, c.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findTrendingIds(UUID viewerId, int limit);

    /**
     * Suggestions: circles in the user's interest categories that they haven't joined and that have
     * room, public before private, then by momentum.
     */
    @Query(value = """
            SELECT c.id FROM circles c
            JOIN user_interests ui ON ui.interest_id = c.category_id AND ui.user_id = :userId
            LEFT JOIN (
                SELECT circle_id, count(*) AS n FROM circle_members
                WHERE status = 'active' AND created_at > now() - interval '7 days'
                GROUP BY circle_id
            ) recent ON recent.circle_id = c.id
            WHERE c.member_count < c.max_members
              AND NOT EXISTS (SELECT 1 FROM circle_members m WHERE m.circle_id = c.id AND m.user_id = :userId)
            ORDER BY c.is_private ASC, coalesce(recent.n, 0) * 3 + c.member_count DESC, c.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findSuggestedIds(UUID userId, int limit);
}
