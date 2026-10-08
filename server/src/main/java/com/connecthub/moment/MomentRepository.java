package com.connecthub.moment;

import com.connecthub.circle.MemberStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MomentRepository extends JpaRepository<Moment, UUID> {

    /**
     * Live moments visible to a viewer: their own, plus those of anyone who shares an active circle
     * with them. Oldest first so each person's moments play in order.
     */
    @EntityGraph(attributePaths = "author")
    @Query("""
            select m from Moment m where m.archived = false and m.expiresAt > :now
              and (m.author.id = :viewerId or exists (
                    select 1 from CircleMember x, CircleMember y
                    where x.circle.id = y.circle.id and x.user.id = :viewerId and y.user.id = m.author.id
                      and x.status = :active and y.status = :active))
            order by m.createdAt asc, m.id asc
            """)
    List<Moment> findLiveVisibleTo(UUID viewerId, Instant now, MemberStatus active);

    /** The author's Rewind: archived moments, newest first, keyset on (created_at, id). */
    @Query("""
            select m from Moment m where m.author.id = :authorId and m.archived = true
              and (m.createdAt < :ts or (m.createdAt = :ts and m.id < :id))
            order by m.createdAt desc, m.id desc
            """)
    List<Moment> findRewindPage(UUID authorId, Instant ts, UUID id, Limit limit);

    @Modifying
    @Query("update Moment m set m.archived = true where m.archived = false and m.expiresAt <= :now")
    int archiveExpired(Instant now);

    @Modifying
    @Query(value = "UPDATE moments SET view_count = view_count + 1 WHERE id = :id", nativeQuery = true)
    void incrementViews(UUID id);

    @Modifying
    @Query(value = """
            INSERT INTO moment_views (moment_id, viewer_id) VALUES (:momentId, :viewerId)
            ON CONFLICT (moment_id, viewer_id) DO NOTHING
            """, nativeQuery = true)
    int recordView(UUID momentId, UUID viewerId);

    @Query(value = "SELECT moment_id FROM moment_views WHERE viewer_id = :viewerId AND moment_id IN (:ids)", nativeQuery = true)
    List<UUID> findSeen(UUID viewerId, Collection<UUID> ids);
}
