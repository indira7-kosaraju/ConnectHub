package com.connecthub.comment;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @EntityGraph(attributePaths = {"author", "post", "post.circle", "parent"})
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findDetailedById(UUID id);

    /** Top-level comments, oldest first (conversation order), keyset on (created_at, id). */
    @EntityGraph(attributePaths = "author")
    @Query("""
            select c from Comment c where c.post.id = :postId and c.parent is null
              and (c.createdAt > :ts or (c.createdAt = :ts and c.id > :id))
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findTopLevelPage(UUID postId, Instant ts, UUID id, Limit limit);

    @EntityGraph(attributePaths = "author")
    @Query("select c from Comment c where c.parent.id in :parentIds order by c.createdAt asc, c.id asc")
    List<Comment> findReplies(Collection<UUID> parentIds);

    long countByParentId(UUID parentId);
}
