package com.connecthub.post;

import com.connecthub.circle.Circle;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.circle.CircleRepository;
import com.connecthub.circle.CircleService;
import com.connecthub.common.CursorPage;
import com.connecthub.common.PageCursor;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.moderation.ReportRequest;
import com.connecthub.moderation.ReportService;
import com.connecthub.moderation.ReportTarget;
import com.connecthub.post.dto.CreatePostRequest;
import com.connecthub.post.dto.PostDto;
import com.connecthub.post.dto.ReactionResult;
import com.connecthub.post.dto.SavedResult;
import com.connecthub.post.dto.UpdatePostRequest;
import com.connecthub.post.dto.VibeMix;
import com.connecthub.post.event.PostReactedEvent;
import com.connecthub.storage.ImagePreset;
import com.connecthub.storage.StorageService;
import com.connecthub.storage.StoredImage;
import com.connecthub.user.ProfileService;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final UUID NO_CIRCLE = new UUID(0, 0);

    private final PostRepository posts;
    private final ReactionRepository reactions;
    private final SavedPostRepository saved;
    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final CircleService circleService;
    private final ProfileService profiles;
    private final UserRepository users;
    private final StorageService storage;
    private final PostAccess access;
    private final PostAssembler assembler;
    private final ReportService reports;
    private final ApplicationEventPublisher events;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.moderation.ProfanityFilter profanity;

    // ------------------------------------------------------------------ write

    @Transactional
    public PostDto create(UUID userId, CreatePostRequest req, List<MultipartFile> files) {
        List<MultipartFile> images = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (images.size() > Post.MAX_IMAGES) {
            throw new BadRequestException("Up to " + Post.MAX_IMAGES + " images per post");
        }
        String content = profanity.clean(TextSanitizer.plain(req.content()));
        if (content == null && images.isEmpty()) {
            throw new BadRequestException("Write something or add a photo");
        }
        Circle circle = circles.findById(req.circleId()).orElseThrow(() -> new NotFoundException("Circle not found"));
        access.requireParticipant(circle, userId);

        Post post = new Post();
        post.setAuthor(users.getReferenceById(userId));
        post.setCircle(circle);
        post.setContent(content);
        for (int i = 0; i < images.size(); i++) {
            StoredImage stored = storage.store(images.get(i), ImagePreset.POST);
            post.getImages().add(new PostImage(post, stored.url(), stored.width(), stored.height(), i));
        }
        posts.saveAndFlush(post);
        return assembler.toDto(post, userId);
    }

    @Transactional
    public PostDto update(UUID postId, UUID userId, UpdatePostRequest req) {
        Post post = load(postId);
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own posts");
        }
        if (!post.isEditable(Instant.now())) {
            throw new ForbiddenException("Posts can only be edited within 15 minutes");
        }
        String content = profanity.clean(TextSanitizer.plain(req.content()));
        if (content == null && post.getImages().isEmpty()) {
            throw new BadRequestException("A post needs text or a photo");
        }
        if (!java.util.Objects.equals(content, post.getContent())) {
            post.setContent(content);
            post.setEditedAt(Instant.now());
        }
        return assembler.toDto(post, userId);
    }

    /** Authors delete their posts; circle admins can remove any post in their circle. */
    @Transactional
    public void delete(UUID postId, UUID userId) {
        Post post = load(postId);
        if (!post.getAuthor().getId().equals(userId) && !access.isCircleAdmin(post.getCircle(), userId)) {
            throw new ForbiddenException("You can't delete this post");
        }
        posts.delete(post);
    }

    @Transactional
    public ReactionResult react(UUID postId, UUID userId, ReactionType type) {
        Post post = load(postId);
        access.requireReadable(post, userId);
        access.requireParticipant(post.getCircle(), userId);
        String previous = reactions.upsert(postId, userId, type.name());
        if (previous == null) {
            posts.addReactions(postId, 1);
            if (!post.getAuthor().getId().equals(userId)) {
                events.publishEvent(new PostReactedEvent(postId, post.getAuthor().getId(), userId, type));
            }
        }
        return new ReactionResult(vibe(postId), type);
    }

    @Transactional
    public ReactionResult unreact(UUID postId, UUID userId) {
        Post post = load(postId);
        access.requireReadable(post, userId);
        if (reactions.deleteByPostAndUser(postId, userId) > 0) {
            posts.addReactions(postId, -1);
        }
        return new ReactionResult(vibe(postId), null);
    }

    @Transactional
    public SavedResult setSaved(UUID postId, UUID userId, boolean save) {
        Post post = load(postId);
        access.requireReadable(post, userId);
        if (save) {
            saved.save(userId, postId);
        } else {
            saved.unsave(userId, postId);
        }
        return new SavedResult(save);
    }

    @Transactional
    public void report(UUID postId, UUID userId, ReportRequest req) {
        Post post = load(postId);
        access.requireReadable(post, userId);
        if (post.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("You can't report your own post");
        }
        reports.report(userId, ReportTarget.post, postId, req);
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public PostDto get(UUID postId, UUID viewerId) {
        Post post = load(postId);
        access.requireReadable(post, viewerId);
        return assembler.toDto(post, viewerId);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostDto> circlePosts(String slug, UUID viewerId, String cursor, Integer limit) {
        Circle circle = circleService.loadCircle(slug);
        if (!circleService.canViewContent(circle, circleService.membership(circle, viewerId))) {
            throw new ForbiddenException("Join this circle to see its posts");
        }
        PageCursor c = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        return page(posts.findCirclePage(circle.getId(), blocks.hiddenForQuery(viewerId), c.createdAt(), c.id(), Limit.of(n + 1)),
                n, viewerId, Post::getCreatedAt);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostDto> userPosts(String username, UUID viewerId, String cursor, Integer limit) {
        User author = profiles.loadVisible(username, viewerId);
        if (!profiles.canSeeFull(author, viewerId)) {
            throw new ForbiddenException("This profile is private");
        }
        List<UUID> viewerCircles = new ArrayList<>(viewerId == null ? List.of() : members.findActiveCircleIds(viewerId));
        if (author.getId().equals(viewerId)) {
            viewerCircles.addAll(members.findActiveCircleIds(author.getId()));
        }
        if (viewerCircles.isEmpty()) {
            viewerCircles.add(NO_CIRCLE); // `in ()` is not valid SQL
        }
        PageCursor c = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        return page(posts.findAuthorPage(author.getId(), viewerCircles, c.createdAt(), c.id(), Limit.of(n + 1)), n, viewerId, Post::getCreatedAt);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostDto> savedPosts(UUID userId, String cursor, Integer limit) {
        PageCursor c = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        List<SavedPost> rows = saved.findPage(userId, c.createdAt(), c.id(), Limit.of(n + 1));
        boolean more = rows.size() > n;
        List<SavedPost> pageRows = more ? rows.subList(0, n) : rows;
        // A saved post may have moved out of reach (e.g. you left its private circle): skip it.
        java.util.Set<UUID> readable = new java.util.HashSet<>(members.findActiveCircleIds(userId));
        java.util.Set<UUID> hidden = blocks.hiddenFrom(userId);
        List<Post> visible = pageRows.stream().map(SavedPost::getPost)
                .filter(p -> !p.getCircle().isPrivateCircle() || readable.contains(p.getCircle().getId()))
                .filter(p -> !hidden.contains(p.getAuthor().getId()))
                .toList();
        String next = more ? new PageCursor(pageRows.get(n - 1).getCreatedAt(), pageRows.get(n - 1).getId()).encode() : null;
        return new CursorPage<>(assembler.toDtos(visible, userId), next);
    }

    // ------------------------------------------------------------------ helpers

    public Post load(UUID postId) {
        return posts.findDetailedById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
    }

    private VibeMix vibe(UUID postId) {
        return assembler.vibes(List.of(postId)).getOrDefault(postId, VibeMix.EMPTY);
    }

    private CursorPage<PostDto> page(List<Post> rows, int n, UUID viewerId, Function<Post, Instant> key) {
        boolean more = rows.size() > n;
        List<Post> pagePosts = more ? rows.subList(0, n) : rows;
        String next = more ? new PageCursor(key.apply(pagePosts.get(n - 1)), pagePosts.get(n - 1).getId()).encode() : null;
        return new CursorPage<>(assembler.toDtos(pagePosts, viewerId), next);
    }
}
