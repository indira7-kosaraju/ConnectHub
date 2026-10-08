package com.connecthub.moderation;

import com.connecthub.common.exception.BadRequestException;
import com.connecthub.user.UserRepository;
import com.connecthub.user.dto.UserSummary;
import com.connecthub.circle.CircleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Blocking hides content <b>both ways</b>: neither person sees the other's posts, comments,
 * moments or profile, they can't message each other, and neither is notified about the other.
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    /** Stand-in so {@code NOT IN (:ids)} is valid SQL when nobody is blocked. */
    public static final UUID NOBODY = new UUID(0, 0);

    private final BlockRepository blocks;
    private final UserRepository users;
    private final CircleMapper mapper;

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BadRequestException("You can't block yourself");
        }
        blocks.block(blockerId, blockedId);
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        blocks.unblock(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> blockedBy(UUID blockerId) {
        return blocks.findByBlockerIdOrderByCreatedAtDesc(blockerId).stream().map(b -> mapper.toUserSummary(b.getBlocked())).toList();
    }

    /** Users hidden from {@code viewerId} (either direction). Empty for anonymous viewers. */
    @Transactional(readOnly = true)
    public Set<UUID> hiddenFrom(UUID viewerId) {
        return viewerId == null ? Set.of() : new HashSet<>(blocks.findHiddenUserIds(viewerId));
    }

    /** {@link #hiddenFrom} as a never-empty collection for {@code NOT IN} clauses. */
    @Transactional(readOnly = true)
    public Collection<UUID> hiddenForQuery(UUID viewerId) {
        List<UUID> ids = new ArrayList<>(hiddenFrom(viewerId));
        if (ids.isEmpty()) {
            ids.add(NOBODY);
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public boolean blockedEitherWay(UUID a, UUID b) {
        return a != null && b != null && blocks.blockedEitherWay(a, b);
    }
}
