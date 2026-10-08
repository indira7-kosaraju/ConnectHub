package com.connecthub.circle;

import com.connecthub.circle.dto.CircleDetail;
import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.circle.dto.CreateCircleRequest;
import com.connecthub.circle.dto.JoinResult;
import com.connecthub.circle.dto.MemberDto;
import com.connecthub.circle.dto.UpdateCircleRequest;
import com.connecthub.circle.dto.ViewerMembership;
import com.connecthub.circle.event.CircleJoinApprovedEvent;
import com.connecthub.circle.event.CircleJoinRequestedEvent;
import com.connecthub.common.PageResponse;
import com.connecthub.common.Slugs;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ConflictException;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.storage.ImagePreset;
import com.connecthub.storage.StorageService;
import com.connecthub.user.Interest;
import com.connecthub.user.InterestRepository;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CircleService {

    /** Anonymous viewers never match a membership row. */
    private static final UUID NOBODY = new UUID(0, 0);

    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final InterestRepository interests;
    private final UserRepository users;
    private final StorageService storage;
    private final CircleMapper mapper;
    private final ApplicationEventPublisher events;
    private final com.connecthub.post.PostRepository posts;
    private final com.connecthub.moderation.ProfanityFilter profanity;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.search.SearchQueries search;

    // ------------------------------------------------------------------ create / edit

    @Transactional
    public CircleDetail create(UUID userId, CreateCircleRequest req) {
        User creator = users.getReferenceById(userId);
        String name = requireName(req.name());

        Circle circle = new Circle();
        circle.setName(name);
        circle.setSlug(Slugs.unique(Slugs.slugify(name), circles::existsBySlug));
        circle.setDescription(profanity.clean(TextSanitizer.plain(req.description())));
        circle.setCategory(loadCategory(req.categoryId()));
        circle.setPrivateCircle(req.privateCircle());
        circle.setMaxMembers(req.maxMembers() == null ? Circle.MAX_MEMBERS : req.maxMembers());
        circle.setMemberCount(1);
        circle.setCreatedBy(creator);
        circles.save(circle);
        members.save(new CircleMember(circle, creator, CircleRole.admin, MemberStatus.active));
        return detail(circle, userId);
    }

    @Transactional
    public CircleDetail update(String slug, UUID userId, UpdateCircleRequest req) {
        Circle circle = lockCircle(slug);
        requireAdmin(circle, userId);
        if (req.name() != null) {
            circle.setName(requireName(req.name()));
        }
        if (req.description() != null) {
            circle.setDescription(profanity.clean(TextSanitizer.plain(req.description())));
        }
        if (req.categoryId() != null) {
            circle.setCategory(loadCategory(req.categoryId()));
        }
        if (req.privateCircle() != null) {
            circle.setPrivateCircle(req.privateCircle());
        }
        if (req.maxMembers() != null) {
            if (req.maxMembers() < circle.getMemberCount()) {
                throw new BadRequestException("The circle already has " + circle.getMemberCount() + " members");
            }
            circle.setMaxMembers(req.maxMembers());
        }
        return detail(circle, userId);
    }

    @Transactional
    public CircleDetail uploadCover(String slug, UUID userId, MultipartFile file) {
        Circle circle = lockCircle(slug);
        requireAdmin(circle, userId);
        circle.setCoverUrl(storage.uploadImage(file, ImagePreset.CIRCLE_COVER));
        return detail(circle, userId);
    }

    /** Pin a post from this circle (admins). Replaces any existing pin. */
    @Transactional
    public CircleDetail pin(String slug, UUID adminId, UUID postId) {
        Circle circle = lockCircle(slug);
        requireAdmin(circle, adminId);
        var post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        if (!post.getCircle().getId().equals(circle.getId())) {
            throw new BadRequestException("That post belongs to another circle");
        }
        circle.setPinnedPostId(postId);
        return detail(circle, adminId);
    }

    @Transactional
    public CircleDetail unpin(String slug, UUID adminId) {
        Circle circle = lockCircle(slug);
        requireAdmin(circle, adminId);
        circle.setPinnedPostId(null);
        return detail(circle, adminId);
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public CircleDetail get(String slug, UUID viewerId) {
        return detail(loadCircle(slug), viewerId);
    }

    @Transactional(readOnly = true)
    public PageResponse<CircleSummary> browse(String categorySlug, String query, int page, int size, UUID viewerId) {
        String ts = com.connecthub.search.SearchQueries.toPrefixQuery(query);
        if (ts != null) {
            // Text search: ranked full-text results (V5 search_vector), optionally within a category.
            int n = clamp(size, 24);
            int p = Math.max(0, page);
            List<UUID> ids = search.circles(ts, categorySlug, n, p * n);
            long total = search.countCircles(ts, categorySlug);
            List<CircleSummary> items = withViewer(summariesInOrder(ids), viewerId);
            return new PageResponse<>(items, p, n, total, (long) (p + 1) * n < total);
        }
        Specification<Circle> spec = (root, cq, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (categorySlug != null && !categorySlug.isBlank()) {
                where.add(cb.equal(root.get("category").get("slug"), categorySlug));
            }
            if (cq.getResultType() != Long.class) {
                root.fetch("category");
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        Pageable pageable = PageRequest.of(Math.max(0, page), clamp(size, 24),
                Sort.by(Sort.Order.desc("memberCount"), Sort.Order.desc("createdAt")));
        PageResponse<CircleSummary> result = PageResponse.of(circles.findAll(spec, pageable), mapper::toSummary);
        return new PageResponse<>(withViewer(result.items(), viewerId), result.page(), result.size(),
                result.totalElements(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public List<CircleSummary> trending(UUID viewerId, int limit) {
        return summariesInOrder(circles.findTrendingIds(viewerId == null ? NOBODY : viewerId, clamp(limit, 24)));
    }

    /** Interest-matched circles, topped up with trending ones the user hasn't joined. */
    @Transactional(readOnly = true)
    public List<CircleSummary> suggestions(UUID userId, int limit) {
        int n = clamp(limit, 12);
        Set<UUID> ids = new LinkedHashSet<>(circles.findSuggestedIds(userId, n));
        if (ids.size() < n) {
            for (UUID id : circles.findTrendingIds(userId, n * 2)) {
                if (ids.size() >= n) break;
                ids.add(id);
            }
        }
        return summariesInOrder(new ArrayList<>(ids)).stream().filter(c -> !c.full()).toList();
    }

    @Transactional(readOnly = true)
    public List<CircleSummary> myCircles(UUID userId) {
        return members.findActiveByUser(userId).stream()
                .map(m -> mapper.toSummary(m.getCircle()).withViewerStatus(MemberStatus.active)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberDto> members(String slug, UUID viewerId, MemberStatus status, int page, int size) {
        Circle circle = loadCircle(slug);
        CircleMember viewer = membership(circle, viewerId);
        if (status == MemberStatus.pending) {
            if (viewer == null || !viewer.isAdmin()) {
                throw new ForbiddenException("Only admins can see join requests");
            }
        } else if (!canViewContent(circle, viewer)) {
            throw new ForbiddenException("Join this circle to see its members");
        }
        // Admins first, then by join date.
        Pageable pageable = PageRequest.of(Math.max(0, page), clamp(size, 50),
                Sort.by(Sort.Order.asc("role"), Sort.Order.asc("createdAt")));
        java.util.Set<UUID> hidden = blocks.hiddenFrom(viewerId);
        PageResponse<MemberDto> result = PageResponse.of(members.findByCircleIdAndStatus(circle.getId(), status, pageable), mapper::toMember);
        return new PageResponse<>(result.items().stream().filter(m -> !hidden.contains(m.user().id())).toList(),
                result.page(), result.size(), result.totalElements(), result.hasNext());
    }

    // ------------------------------------------------------------------ membership

    @Transactional
    public JoinResult join(String slug, UUID userId) {
        Circle circle = lockCircle(slug);
        CircleMember existing = membership(circle, userId);
        if (existing != null) {
            return new JoinResult(view(existing), circle.getMemberCount()); // idempotent
        }
        User user = users.getReferenceById(userId);
        if (circle.isPrivateCircle()) {
            CircleMember request = members.save(new CircleMember(circle, user, CircleRole.member, MemberStatus.pending));
            events.publishEvent(new CircleJoinRequestedEvent(circle.getId(), userId));
            return new JoinResult(view(request), circle.getMemberCount());
        }
        requireRoom(circle);
        CircleMember member = members.save(new CircleMember(circle, user, CircleRole.member, MemberStatus.active));
        circle.setMemberCount(circle.getMemberCount() + 1);
        return new JoinResult(view(member), circle.getMemberCount());
    }

    /**
     * Leave (or cancel a pending request). If the last admin leaves, the longest-standing member is
     * promoted; if nobody else is left, the circle is deleted.
     */
    @Transactional
    public void leave(String slug, UUID userId) {
        Circle circle = lockCircle(slug);
        CircleMember m = membership(circle, userId);
        if (m == null) {
            throw new NotFoundException("You're not in this circle");
        }
        boolean lastAdmin = m.isAdmin()
                && members.countByCircleIdAndRoleAndStatus(circle.getId(), CircleRole.admin, MemberStatus.active) == 1;
        var successor = lastAdmin
                ? members.findFirstByCircleIdAndStatusAndUserIdNotOrderByCreatedAtAsc(circle.getId(), MemberStatus.active, userId)
                : java.util.Optional.<CircleMember>empty();

        members.delete(m);
        if (!m.isActive()) {
            return;
        }
        if (lastAdmin && successor.isEmpty()) {
            circles.delete(circle); // nobody left
            return;
        }
        successor.ifPresent(next -> next.setRole(CircleRole.admin));
        circle.setMemberCount(Math.max(0, circle.getMemberCount() - 1));
    }

    @Transactional
    public MemberDto approve(String slug, UUID adminId, UUID userId) {
        Circle circle = lockCircle(slug);
        requireAdmin(circle, adminId);
        CircleMember m = membership(circle, userId);
        if (m == null || m.isActive()) {
            throw new NotFoundException("No pending request from that user");
        }
        requireRoom(circle);
        m.setStatus(MemberStatus.active);
        circle.setMemberCount(circle.getMemberCount() + 1);
        events.publishEvent(new CircleJoinApprovedEvent(circle.getId(), userId, adminId));
        return mapper.toMember(m);
    }

    /** Remove an active member or decline a pending request. Admins can't remove other admins. */
    @Transactional
    public void removeMember(String slug, UUID adminId, UUID userId) {
        Circle circle = lockCircle(slug);
        requireAdmin(circle, adminId);
        if (adminId.equals(userId)) {
            throw new BadRequestException("Use leave to remove yourself");
        }
        CircleMember m = membership(circle, userId);
        if (m == null) {
            throw new NotFoundException("That user isn't in this circle");
        }
        if (m.isAdmin()) {
            throw new ForbiddenException("Admins can't remove other admins");
        }
        members.delete(m);
        if (m.isActive()) {
            circle.setMemberCount(Math.max(0, circle.getMemberCount() - 1));
        }
    }

    // ------------------------------------------------------------------ helpers (also used by posts/feed)

    /** Throws 404 for unknown circles. */
    public Circle loadCircle(String slug) {
        return circles.findBySlug(slug).orElseThrow(() -> new NotFoundException("Circle not found"));
    }

    public CircleMember membership(Circle circle, UUID userId) {
        return userId == null ? null : members.findByCircleIdAndUserId(circle.getId(), userId).orElse(null);
    }

    public boolean canViewContent(Circle circle, CircleMember viewer) {
        return !circle.isPrivateCircle() || (viewer != null && viewer.isActive());
    }

    private CircleDetail detail(Circle circle, UUID viewerId) {
        CircleMember viewer = membership(circle, viewerId);
        var admins = members.findByCircleIdAndRoleAndStatusOrderByCreatedAtAsc(circle.getId(), CircleRole.admin, MemberStatus.active)
                .stream().map(a -> mapper.toUserSummary(a.getUser())).toList();
        long pending = viewer != null && viewer.isAdmin()
                ? members.countByCircleIdAndStatus(circle.getId(), MemberStatus.pending) : 0;
        CircleSummary s = mapper.toSummary(circle);
        return new CircleDetail(s.id(), s.slug(), s.name(), s.description(), s.coverUrl(), s.category(), s.privateCircle(),
                s.memberCount(), s.maxMembers(), s.full(), circle.getPinnedPostId(), circle.getCreatedAt(), admins,
                viewer == null ? null : view(viewer), canViewContent(circle, viewer), pending);
    }

    /** Fills {@code viewerStatus} with one query for the whole list. */
    public List<CircleSummary> withViewer(List<CircleSummary> list, UUID viewerId) {
        if (viewerId == null || list.isEmpty()) {
            return list;
        }
        Map<UUID, MemberStatus> statuses = members.findByUserIdAndCircleIds(viewerId, list.stream().map(CircleSummary::id).toList())
                .stream().collect(Collectors.toMap(m -> m.getCircle().getId(), CircleMember::getStatus));
        return list.stream().map(c -> c.withViewerStatus(statuses.get(c.id()))).toList();
    }

    private List<CircleSummary> summariesInOrder(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Circle> byId = circles.findAllByIdIn(ids).stream().collect(Collectors.toMap(Circle::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).map(mapper::toSummary).toList();
    }

    private Circle lockCircle(String slug) {
        return circles.findBySlugForUpdate(slug).orElseThrow(() -> new NotFoundException("Circle not found"));
    }

    private void requireAdmin(Circle circle, UUID userId) {
        CircleMember m = membership(circle, userId);
        if (m == null || !m.isAdmin()) {
            throw new ForbiddenException("Only circle admins can do that");
        }
    }

    private static void requireRoom(Circle circle) {
        if (circle.isFull()) {
            throw new ConflictException("This circle is full (" + circle.getMaxMembers() + " members)");
        }
    }

    private Interest loadCategory(UUID id) {
        return interests.findById(id).orElseThrow(() -> new BadRequestException("Unknown category"));
    }

    private String requireName(String raw) {
        String name = TextSanitizer.line(raw);
        if (name == null || name.length() < 3) {
            throw new BadRequestException("Circle names need at least 3 characters");
        }
        if (profanity.isProfane(name)) {
            throw new BadRequestException("Please choose a different name");
        }
        return name;
    }

    private static ViewerMembership view(CircleMember m) {
        return new ViewerMembership(m.getRole(), m.getStatus());
    }

    private static int clamp(int value, int max) {
        return Math.max(1, Math.min(value, max));
    }

}
