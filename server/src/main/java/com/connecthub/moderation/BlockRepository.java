package com.connecthub.moderation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {

    @Modifying
    @Query(value = "INSERT INTO blocks (blocker_id, blocked_id) VALUES (:blocker, :blocked) ON CONFLICT DO NOTHING", nativeQuery = true)
    int block(UUID blocker, UUID blocked);

    @Modifying
    @Query("delete from Block b where b.blocker.id = :blocker and b.blocked.id = :blocked")
    int unblock(UUID blocker, UUID blocked);

    /** Everyone on the other side of a block with {@code userId}, in either direction. */
    @Query(value = """
            SELECT blocked_id FROM blocks WHERE blocker_id = :userId
            UNION
            SELECT blocker_id FROM blocks WHERE blocked_id = :userId
            """, nativeQuery = true)
    List<UUID> findHiddenUserIds(UUID userId);

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM blocks
                           WHERE (blocker_id = :a AND blocked_id = :b) OR (blocker_id = :b AND blocked_id = :a))
            """, nativeQuery = true)
    boolean blockedEitherWay(UUID a, UUID b);

    @EntityGraph(attributePaths = "blocked")
    List<Block> findByBlockerIdOrderByCreatedAtDesc(UUID blockerId);
}
