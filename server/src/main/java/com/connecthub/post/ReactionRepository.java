package com.connecthub.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    /**
     * Insert or change a reaction in one statement (one per user per post).
     * Returns the previous type, or null if this was a new reaction.
     */
    @Query(value = """
            WITH prev AS (SELECT type FROM reactions WHERE user_id = :userId AND post_id = :postId)
            INSERT INTO reactions (post_id, user_id, type) VALUES (:postId, :userId, CAST(:type AS reaction_type))
            ON CONFLICT (user_id, post_id) DO UPDATE SET type = EXCLUDED.type
            RETURNING (SELECT CAST(type AS text) FROM prev)
            """, nativeQuery = true)
    String upsert(UUID postId, UUID userId, String type);

    @Modifying
    @Query("delete from Reaction r where r.post.id = :postId and r.user.id = :userId")
    int deleteByPostAndUser(UUID postId, UUID userId);

    /** Rows: [postId, type, count]. */
    @Query("select r.post.id, r.type, count(r) from Reaction r where r.post.id in :postIds group by r.post.id, r.type")
    List<Object[]> countByType(Collection<UUID> postIds);

    /** Rows: [postId, userId, type] for the given posts and users. */
    @Query("select r.post.id, r.user.id, r.type from Reaction r where r.post.id in :postIds and r.user.id in :userIds")
    List<Object[]> findTypes(Collection<UUID> postIds, Collection<UUID> userIds);

    /** Rows: [postId, type]. */
    @Query("select r.post.id, r.type from Reaction r where r.user.id = :userId and r.post.id in :postIds")
    List<Object[]> findMine(UUID userId, Collection<UUID> postIds);
}
