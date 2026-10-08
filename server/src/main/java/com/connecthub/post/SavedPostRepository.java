package com.connecthub.post;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SavedPostRepository extends JpaRepository<SavedPost, UUID> {

    @Modifying
    @Query(value = "INSERT INTO saved_posts (user_id, post_id) VALUES (:userId, :postId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int save(UUID userId, UUID postId);

    @Modifying
    @Query("delete from SavedPost s where s.user.id = :userId and s.post.id = :postId")
    int unsave(UUID userId, UUID postId);

    @Query("select s.post.id from SavedPost s where s.user.id = :userId and s.post.id in :postIds")
    List<UUID> findSavedIds(UUID userId, Collection<UUID> postIds);

    /** Newest saves first, keyset on (saved_posts.created_at, saved_posts.id). */
    @EntityGraph(attributePaths = {"post", "post.author", "post.circle"})
    @Query("""
            select s from SavedPost s where s.user.id = :userId
              and (s.createdAt < :ts or (s.createdAt = :ts and s.id < :id))
            order by s.createdAt desc, s.id desc
            """)
    List<SavedPost> findPage(UUID userId, Instant ts, UUID id, Limit limit);
}
