package com.connecthub.moment;

import com.connecthub.circle.CircleMapper;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.circle.MemberStatus;
import com.connecthub.common.CursorPage;
import com.connecthub.common.PageCursor;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.moment.dto.CreateMomentRequest;
import com.connecthub.moment.dto.MomentDto;
import com.connecthub.moment.dto.MomentGroup;
import com.connecthub.storage.ImagePreset;
import com.connecthub.storage.StorageService;
import com.connecthub.storage.StoredImage;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Moments: 24-hour posts seen by the people you share circles with, then archived into your
 * private Rewind.
 */
@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentRepository moments;
    private final CircleMemberRepository members;
    private final UserRepository users;
    private final StorageService storage;
    private final CircleMapper mapper;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.moderation.ProfanityFilter profanity;

    @Transactional
    public MomentDto create(UUID userId, CreateMomentRequest req, MultipartFile image) {
        Moment m = new Moment();
        m.setAuthor(users.getReferenceById(userId));
        m.setKind(req.kind());
        if (req.kind() == MomentKind.image) {
            if (image == null || image.isEmpty()) {
                throw new BadRequestException("Choose a photo for your moment");
            }
            StoredImage stored = storage.store(image, ImagePreset.MOMENT);
            m.setImageUrl(stored.url());
            m.setImageWidth(stored.width());
            m.setImageHeight(stored.height());
            m.setText(profanity.clean(TextSanitizer.line(req.text()))); // optional caption
        } else {
            String text = profanity.clean(TextSanitizer.plain(req.text()));
            if (text == null) {
                throw new BadRequestException("Write something for your moment");
            }
            m.setText(text);
            m.setBackground((req.background() == null ? MomentBackground.sunset : req.background()).name());
        }
        Instant now = Instant.now();
        m.setExpiresAt(now.plus(Moment.LIFETIME));
        moments.saveAndFlush(m);
        return toDto(m, userId, Set.of());
    }

    /** The "Today" strip: you first, then people with unseen moments, then the rest — freshest first. */
    @Transactional(readOnly = true)
    public List<MomentGroup> strip(UUID viewerId) {
        Set<UUID> hidden = blocks.hiddenFrom(viewerId);
        List<Moment> live = moments.findLiveVisibleTo(viewerId, Instant.now(), MemberStatus.active).stream()
                .filter(m -> !hidden.contains(m.getAuthor().getId())).toList();
        if (live.isEmpty()) {
            return List.of();
        }
        Set<UUID> seen = new HashSet<>(moments.findSeen(viewerId, live.stream().map(Moment::getId).toList()));
        Map<UUID, List<Moment>> byAuthor = new LinkedHashMap<>();
        for (Moment m : live) {
            byAuthor.computeIfAbsent(m.getAuthor().getId(), k -> new ArrayList<>()).add(m);
        }
        List<MomentGroup> groups = new ArrayList<>();
        byAuthor.forEach((authorId, list) -> {
            boolean mine = authorId.equals(viewerId);
            List<MomentDto> dtos = list.stream().map(m -> toDto(m, viewerId, seen)).toList();
            groups.add(new MomentGroup(mapper.toUserSummary(list.get(0).getAuthor()), mine,
                    dtos.stream().allMatch(MomentDto::seen), dtos));
        });
        groups.sort(Comparator.comparing((MomentGroup g) -> !g.mine())
                .thenComparing(MomentGroup::allSeen)
                .thenComparing(g -> g.moments().get(g.moments().size() - 1).createdAt(), Comparator.reverseOrder()));
        return groups;
    }

    /** Marks a moment seen by the viewer; the first view by each person counts once. */
    @Transactional
    public void view(UUID momentId, UUID viewerId) {
        Moment m = load(momentId);
        if (m.getAuthor().getId().equals(viewerId)) {
            return;
        }
        if (!m.isLive(Instant.now()) || !members.shareActiveCircle(m.getAuthor().getId(), viewerId)
                || blocks.blockedEitherWay(viewerId, m.getAuthor().getId())) {
            throw new NotFoundException("Moment not found");
        }
        if (moments.recordView(momentId, viewerId) > 0) {
            moments.incrementViews(momentId);
        }
    }

    @Transactional
    public void delete(UUID momentId, UUID userId) {
        Moment m = load(momentId);
        if (!m.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own moments");
        }
        moments.delete(m);
    }

    /** Your archived moments — visible only to you. */
    @Transactional(readOnly = true)
    public CursorPage<MomentDto> rewind(UUID userId, String cursor, Integer limit) {
        PageCursor c = PageCursor.parseOr(cursor, PageCursor.NEWEST);
        int n = PageCursor.clampLimit(limit);
        List<Moment> rows = moments.findRewindPage(userId, c.createdAt(), c.id(), Limit.of(n + 1));
        boolean more = rows.size() > n;
        List<Moment> page = more ? rows.subList(0, n) : rows;
        String next = more ? new PageCursor(page.get(n - 1).getCreatedAt(), page.get(n - 1).getId()).encode() : null;
        return new CursorPage<>(page.stream().map(m -> toDto(m, userId, Set.of())).toList(), next);
    }

    /** Called by {@link MomentExpiryScheduler}. */
    @Transactional
    public int archiveExpired() {
        return moments.archiveExpired(Instant.now());
    }

    private Moment load(UUID id) {
        return moments.findById(id).orElseThrow(() -> new NotFoundException("Moment not found"));
    }

    private static MomentDto toDto(Moment m, UUID viewerId, Set<UUID> seen) {
        boolean mine = m.getAuthor().getId().equals(viewerId);
        return new MomentDto(m.getId(), m.getKind(), m.getImageUrl(), m.getImageWidth(), m.getImageHeight(), m.getText(),
                m.getBackground() == null ? null : MomentBackground.valueOf(m.getBackground()),
                m.getCreatedAt(), m.getExpiresAt(), mine || seen.contains(m.getId()), mine ? m.getViewCount() : null);
    }
}
