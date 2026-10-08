package com.connecthub.comment;

import com.connecthub.circle.CircleMapper;
import com.connecthub.comment.dto.CommentDto;
import com.connecthub.comment.dto.CreateCommentRequest;
import com.connecthub.comment.event.CommentCreatedEvent;
import com.connecthub.common.CursorPage;
import com.connecthub.common.PageCursor;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.post.Post;
import com.connecthub.post.PostAccess;
import com.connecthub.post.PostAssembler;
import com.connecthub.post.PostRepository;
import com.connecthub.post.PostService;
import com.connecthub.post.ReactionType;
import com.connecthub.post.dto.ReactionResult;
import com.connecthub.post.dto.VibeMix;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository comments;
    private final CommentReactionRepository reactions;
    private final PostRepository posts;
    private final PostService postService;
    private final PostAccess access;
    private final UserRepository users;
    private final CircleMapper mapper;
    private final ApplicationEventPublisher events;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.moderation.ProfanityFilter profanity;
    private final com.connecthub.moderation.ReportService reports;

    /** Top-level comments oldest-first, each with all of its replies. */
    @Transactional(readOnly = true)
    public CursorPage<CommentDto> list(UUID postId, UUID viewerId, String cursor, Integer limit) {
        Post post = postService.load(postId);
        access.requireReadable(post, viewerId);
        PageCursor c = PageCursor.parseOr(cursor, PageCursor.OLDEST);
        int n = PageCursor.clampLimit(limit);
        List<Comment> rows = comments.findTopLevelPage(postId, c.createdAt(), c.id(), Limit.of(n + 1));
        boolean more = rows.size() > n;
        List<Comment> top = more ? rows.subList(0, n) : rows;
        String next = more ? new PageCursor(top.get(n - 1).getCreatedAt(), top.get(n - 1).getId()).encode() : null;
        java.util.Set<UUID> hidden = blocks.hiddenFrom(viewerId);
        List<Comment> visible = top.stream().filter(x -> !hidden.contains(x.getAuthor().getId())).toList();
        return new CursorPage<>(assemble(post, visible, viewerId), next);
    }

    @Transactional
    public CommentDto create(UUID postId, UUID userId, CreateCommentRequest req) {
        Post post = postService.load(postId);
        access.requireReadable(post, userId);
        access.requireParticipant(post.getCircle(), userId);
        String content = profanity.clean(TextSanitizer.plain(req.content()));
        if (content == null) {
            throw new BadRequestException("Write something first");
        }
        Comment parent = null;
        if (req.parentId() != null) {
            parent = comments.findDetailedById(req.parentId())
                    .filter(p -> p.getPost().getId().equals(postId))
                    .orElseThrow(() -> new BadRequestException("You're replying to a comment that no longer exists"));
            if (parent.getParent() != null) {
                parent = parent.getParent(); // one level deep: replies to replies join the thread
            }
        }
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(users.getReferenceById(userId));
        comment.setParent(parent);
        comment.setContent(content);
        comments.saveAndFlush(comment);
        posts.addComments(postId, 1);

        events.publishEvent(new CommentCreatedEvent(comment.getId(), postId, post.getAuthor().getId(),
                parent == null ? null : parent.getAuthor().getId(), userId));
        return assemble(post, List.of(comment), userId).get(0);
    }

    /** Comment author, post author, or circle admin. Deleting a top-level comment removes its replies. */
    @Transactional
    public void delete(UUID commentId, UUID userId) {
        Comment comment = comments.findDetailedById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        Post post = comment.getPost();
        boolean allowed = comment.getAuthor().getId().equals(userId)
                || post.getAuthor().getId().equals(userId)
                || access.isCircleAdmin(post.getCircle(), userId);
        if (!allowed) {
            throw new ForbiddenException("You can't delete this comment");
        }
        long removed = 1 + (comment.getParent() == null ? comments.countByParentId(commentId) : 0);
        comments.delete(comment);
        posts.addComments(post.getId(), (int) -removed);
    }

    @Transactional
    public ReactionResult react(UUID commentId, UUID userId, ReactionType type) {
        Comment comment = comments.findDetailedById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        access.requireReadable(comment.getPost(), userId);
        access.requireParticipant(comment.getPost().getCircle(), userId);
        reactions.upsert(commentId, userId, type.name());
        return new ReactionResult(vibe(commentId), type);
    }

    @Transactional
    public ReactionResult unreact(UUID commentId, UUID userId) {
        Comment comment = comments.findDetailedById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        access.requireReadable(comment.getPost(), userId);
        reactions.deleteByCommentAndUser(commentId, userId);
        return new ReactionResult(vibe(commentId), null);
    }

    @Transactional
    public void report(UUID commentId, UUID userId, com.connecthub.moderation.ReportRequest req) {
        Comment comment = comments.findDetailedById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        access.requireReadable(comment.getPost(), userId);
        if (comment.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("You can't report your own comment");
        }
        reports.report(userId, com.connecthub.moderation.ReportTarget.comment, commentId, req);
    }

    private VibeMix vibe(UUID commentId) {
        return PostAssembler.vibeMixes(reactions.countByType(List.of(commentId))).getOrDefault(commentId, VibeMix.EMPTY);
    }

    private List<CommentDto> assemble(Post post, List<Comment> top, UUID viewerId) {
        if (top.isEmpty()) {
            return List.of();
        }
        List<UUID> topIds = top.stream().filter(c -> c.getParent() == null).map(Comment::getId).toList();
        java.util.Set<UUID> hidden = blocks.hiddenFrom(viewerId);
        List<Comment> replies = topIds.isEmpty() ? List.of() : comments.findReplies(topIds).stream()
                .filter(r -> !hidden.contains(r.getAuthor().getId())).toList();

        List<UUID> all = new ArrayList<>(top.stream().map(Comment::getId).toList());
        replies.forEach(r -> all.add(r.getId()));
        Map<UUID, VibeMix> vibes = PostAssembler.vibeMixes(reactions.countByType(all));
        Map<UUID, ReactionType> mine = new HashMap<>();
        if (viewerId != null) {
            for (Object[] row : reactions.findMine(viewerId, all)) {
                mine.put((UUID) row[0], (ReactionType) row[1]);
            }
        }
        boolean moderator = viewerId != null
                && (post.getAuthor().getId().equals(viewerId) || access.isCircleAdmin(post.getCircle(), viewerId));

        Map<UUID, List<CommentDto>> repliesByParent = new HashMap<>();
        for (Comment r : replies) {
            repliesByParent.computeIfAbsent(r.getParent().getId(), k -> new ArrayList<>())
                    .add(toDto(r, vibes, mine, viewerId, moderator, List.of()));
        }
        return top.stream()
                .map(c -> toDto(c, vibes, mine, viewerId, moderator, repliesByParent.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private CommentDto toDto(Comment c, Map<UUID, VibeMix> vibes, Map<UUID, ReactionType> mine, UUID viewerId,
                             boolean moderator, List<CommentDto> replies) {
        return new CommentDto(c.getId(), c.getPost().getId(), c.getParent() == null ? null : c.getParent().getId(),
                mapper.toUserSummary(c.getAuthor()), c.getContent(), c.getCreatedAt(),
                vibes.getOrDefault(c.getId(), VibeMix.EMPTY), mine.get(c.getId()),
                moderator || c.getAuthor().getId().equals(viewerId), replies);
    }
}
