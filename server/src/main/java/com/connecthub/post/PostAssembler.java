package com.connecthub.post;

import com.connecthub.circle.CircleMapper;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.post.dto.CircleRef;
import com.connecthub.post.dto.PostDto;
import com.connecthub.post.dto.PostImageDto;
import com.connecthub.post.dto.VibeMix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns posts into DTOs with a fixed number of queries per page (images, vibe counts, my
 * reactions, saves, admin circles) — never one query per post.
 */
@Component
@RequiredArgsConstructor
public class PostAssembler {

    private final PostImageRepository images;
    private final ReactionRepository reactions;
    private final SavedPostRepository saved;
    private final CircleMemberRepository members;
    private final CircleMapper circleMapper;

    public PostDto toDto(Post post, UUID viewerId) {
        return toDtos(List.of(post), viewerId).get(0);
    }

    public List<PostDto> toDtos(List<Post> posts, UUID viewerId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = posts.stream().map(Post::getId).toList();

        Map<UUID, List<PostImageDto>> imagesByPost = new HashMap<>();
        for (PostImage i : images.findByPostIds(ids)) {
            imagesByPost.computeIfAbsent(i.getPost().getId(), k -> new ArrayList<>())
                    .add(new PostImageDto(i.getUrl(), i.getWidth(), i.getHeight()));
        }
        Map<UUID, VibeMix> vibes = vibes(ids);
        Map<UUID, ReactionType> mine = new HashMap<>();
        Set<UUID> savedIds = new HashSet<>();
        Set<UUID> adminCircles = new HashSet<>();
        if (viewerId != null) {
            for (Object[] row : reactions.findMine(viewerId, ids)) {
                mine.put((UUID) row[0], (ReactionType) row[1]);
            }
            savedIds.addAll(saved.findSavedIds(viewerId, ids));
            adminCircles.addAll(members.findAdminCircleIds(viewerId));
        }

        Instant now = Instant.now();
        return posts.stream().map(p -> {
            boolean author = p.getAuthor().getId().equals(viewerId);
            boolean admin = adminCircles.contains(p.getCircle().getId());
            return new PostDto(
                    p.getId(),
                    circleMapper.toUserSummary(p.getAuthor()),
                    new CircleRef(p.getCircle().getId(), p.getCircle().getSlug(), p.getCircle().getName(), p.getCircle().isPrivateCircle()),
                    p.getContent(),
                    imagesByPost.getOrDefault(p.getId(), List.of()),
                    p.getCreatedAt(),
                    p.getEditedAt(),
                    author && p.isEditable(now) ? p.editableUntil() : null,
                    vibes.getOrDefault(p.getId(), VibeMix.EMPTY),
                    mine.get(p.getId()),
                    p.getCommentCount(),
                    savedIds.contains(p.getId()),
                    p.getId().equals(p.getCircle().getPinnedPostId()),
                    author || admin,
                    admin);
        }).toList();
    }

    public Map<UUID, VibeMix> vibes(Collection<UUID> postIds) {
        return vibeMixes(reactions.countByType(postIds));
    }

    /** Rows of [id, ReactionType, count] → mix per id. Shared with comments. */
    public static Map<UUID, VibeMix> vibeMixes(List<Object[]> rows) {
        Map<UUID, Map<ReactionType, Long>> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.computeIfAbsent((UUID) row[0], k -> new EnumMap<>(ReactionType.class))
                    .put((ReactionType) row[1], (Long) row[2]);
        }
        Map<UUID, VibeMix> result = new HashMap<>();
        counts.forEach((id, c) -> result.put(id, VibeMix.of(c)));
        return result;
    }
}
