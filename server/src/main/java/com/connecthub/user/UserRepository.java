package com.connecthub.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** People who share an active circle with {@code userId}, optionally filtered by name/username. */
    @Query(value = """
            SELECT DISTINCT u.* FROM users u
            JOIN circle_members b ON b.user_id = u.id AND b.status = 'active'
            JOIN circle_members a ON a.circle_id = b.circle_id AND a.status = 'active' AND a.user_id = :userId
            WHERE u.id <> :userId AND u.username IS NOT NULL
              AND (:q = '' OR u.username ILIKE '%' || :q || '%' OR u.display_name ILIKE '%' || :q || '%')
            ORDER BY u.display_name NULLS LAST, u.username
            LIMIT :limit
            """, nativeQuery = true)
    java.util.List<User> findCircleMates(UUID userId, String q, int limit);

    @EntityGraph(attributePaths = "interests")
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithInterestsById(UUID id);
}
