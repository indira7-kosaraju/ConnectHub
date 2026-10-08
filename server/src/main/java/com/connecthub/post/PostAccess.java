package com.connecthub.post;

import com.connecthub.circle.Circle;
import com.connecthub.circle.CircleMember;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Who may see and act on posts. Reading follows the circle: public circles are readable by anyone,
 * private ones only by active members (others get a 404 so existence isn't leaked). Writing — posting,
 * reacting, commenting — requires active membership.
 */
@Component
@RequiredArgsConstructor
public class PostAccess {

    private final CircleMemberRepository members;
    private final com.connecthub.moderation.BlockService blocks;

    public CircleMember membership(Circle circle, UUID userId) {
        return userId == null ? null : members.findByCircleIdAndUserId(circle.getId(), userId).orElse(null);
    }

    public void requireReadable(Post post, UUID viewerId) {
        if (blocks.blockedEitherWay(viewerId, post.getAuthor().getId())) {
            throw new NotFoundException("Post not found");
        }
        Circle circle = post.getCircle();
        if (circle.isPrivateCircle()) {
            CircleMember m = membership(circle, viewerId);
            if (m == null || !m.isActive()) {
                throw new NotFoundException("Post not found");
            }
        }
    }

    public void requireParticipant(Circle circle, UUID userId) {
        CircleMember m = membership(circle, userId);
        if (m == null || !m.isActive()) {
            throw new ForbiddenException("Join " + circle.getName() + " to take part");
        }
    }

    public boolean isCircleAdmin(Circle circle, UUID userId) {
        CircleMember m = membership(circle, userId);
        return m != null && m.isAdmin();
    }
}
