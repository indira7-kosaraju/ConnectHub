package com.connecthub.circle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircleMemberRepository extends JpaRepository<CircleMember, UUID> {

    Optional<CircleMember> findByCircleIdAndUserId(UUID circleId, UUID userId);

    @Query("select m from CircleMember m where m.user.id = :userId and m.circle.id in :circleIds")
    List<CircleMember> findByUserIdAndCircleIds(UUID userId, java.util.Collection<UUID> circleIds);

    @EntityGraph(attributePaths = "user")
    Page<CircleMember> findByCircleIdAndStatus(UUID circleId, MemberStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<CircleMember> findByCircleIdAndRoleAndStatusOrderByCreatedAtAsc(UUID circleId, CircleRole role, MemberStatus status);

    long countByCircleIdAndRoleAndStatus(UUID circleId, CircleRole role, MemberStatus status);

    long countByCircleIdAndStatus(UUID circleId, MemberStatus status);

    /** Longest-standing active member other than {@code excludeUserId} (for admin hand-over). */
    Optional<CircleMember> findFirstByCircleIdAndStatusAndUserIdNotOrderByCreatedAtAsc(UUID circleId, MemberStatus status, UUID excludeUserId);

    // Enum values are bound as parameters: JPQL enum literals would be cast to a guessed PG type name.

    @Query("""
            select m from CircleMember m join fetch m.circle c join fetch c.category
            where m.user.id = :userId and m.status = :status
            order by m.createdAt desc
            """)
    List<CircleMember> findByUserWithCircle(UUID userId, MemberStatus status);

    @Query("select m.circle.id from CircleMember m where m.user.id = :userId and m.status = :status")
    List<UUID> findCircleIds(UUID userId, MemberStatus status);

    @Query("""
            select count(a) > 0 from CircleMember a, CircleMember b
            where a.circle.id = b.circle.id and a.user.id = :userA and b.user.id = :userB
              and a.status = :status and b.status = :status
            """)
    boolean shareCircle(UUID userA, UUID userB, MemberStatus status);

    @Query("select m.circle.id from CircleMember m where m.user.id = :userId and m.status = :status and m.role = :role")
    List<UUID> findCircleIdsWithRole(UUID userId, MemberStatus status, CircleRole role);

    default List<UUID> findAdminCircleIds(UUID userId) {
        return findCircleIdsWithRole(userId, MemberStatus.active, CircleRole.admin);
    }

    /** Active memberships of a user, newest first, with circle + category loaded. */
    default List<CircleMember> findActiveByUser(UUID userId) {
        return findByUserWithCircle(userId, MemberStatus.active);
    }

    default List<UUID> findActiveCircleIds(UUID userId) {
        return findCircleIds(userId, MemberStatus.active);
    }

    /** Do two users share at least one circle where both are active? */
    default boolean shareActiveCircle(UUID userA, UUID userB) {
        return shareCircle(userA, userB, MemberStatus.active);
    }
}
