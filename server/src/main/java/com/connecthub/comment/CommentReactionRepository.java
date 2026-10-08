package com.connecthub.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO comment_reactions (comment_id, user_id, type) VALUES (:commentId, :userId, CAST(:type AS reaction_type))
            ON CONFLICT (user_id, comment_id) DO UPDATE SET type = EXCLUDED.type
            """, nativeQuery = true)
    int upsert(UUID commentId, UUID userId, String type);

    @Modifying
    @Query("delete from CommentReaction r where r.comment.id = :commentId and r.user.id = :userId")
    int deleteByCommentAndUser(UUID commentId, UUID userId);

    /** Rows: [commentId, type, count]. */
    @Query("select r.comment.id, r.type, count(r) from CommentReaction r where r.comment.id in :ids group by r.comment.id, r.type")
    List<Object[]> countByType(Collection<UUID> ids);

    /** Rows: [commentId, type]. */
    @Query("select r.comment.id, r.type from CommentReaction r where r.user.id = :userId and r.comment.id in :ids")
    List<Object[]> findMine(UUID userId, Collection<UUID> ids);
}
