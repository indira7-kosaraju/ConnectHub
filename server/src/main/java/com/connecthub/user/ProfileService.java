package com.connecthub.user;

import com.connecthub.circle.CircleMapper;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.circle.dto.CircleSummary;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ForbiddenException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.storage.ImagePreset;
import com.connecthub.storage.StorageService;
import com.connecthub.user.dto.MeResponse;
import com.connecthub.user.dto.ProfileResponse;
import com.connecthub.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository users;
    private final CircleMemberRepository members;
    private final StorageService storage;
    private final UserMapper userMapper;
    private final CircleMapper circleMapper;
    private final com.connecthub.circle.CircleService circleService;
    private final com.connecthub.moderation.BlockService blocks;
    private final com.connecthub.moderation.ProfanityFilter profanity;

    @Transactional(readOnly = true)
    public ProfileResponse get(String username, UUID viewerId) {
        User user = loadVisible(username, viewerId);
        boolean me = user.getId().equals(viewerId);
        boolean restricted = !canSeeFull(user, viewerId);
        boolean canMessage = !me && viewerId != null && members.shareActiveCircle(user.getId(), viewerId);
        return new ProfileResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(),
                user.getAvatarUrl(), restricted ? null : user.getCoverUrl(), restricted ? null : user.getLocation(),
                restricted ? null : user.getWebsite(), user.getCreatedAt(), user.isPrivateProfile(), me, restricted, canMessage);
    }

    /** Circles the user is active in. Private circles are listed only to people who are also in them. */
    @Transactional(readOnly = true)
    public List<CircleSummary> circles(String username, UUID viewerId) {
        User user = loadVisible(username, viewerId);
        if (!canSeeFull(user, viewerId)) {
            throw new ForbiddenException("This profile is private");
        }
        boolean me = user.getId().equals(viewerId);
        Set<UUID> viewerCircles = viewerId == null || me ? Set.of() : new HashSet<>(members.findActiveCircleIds(viewerId));
        List<CircleSummary> list = members.findActiveByUser(user.getId()).stream()
                .map(m -> m.getCircle())
                .filter(c -> me || !c.isPrivateCircle() || viewerCircles.contains(c.getId()))
                .map(circleMapper::toSummary)
                .toList();
        return circleService.withViewer(list, viewerId);
    }

    @Transactional
    public MeResponse update(UUID userId, UpdateProfileRequest req) {
        User user = load(userId);
        if (req.displayName() != null) {
            String name = TextSanitizer.line(req.displayName());
            if (name == null) {
                throw new BadRequestException("Display name can't be empty");
            }
            user.setDisplayName(profanity.clean(name));
        }
        if (req.bio() != null) {
            user.setBio(profanity.clean(TextSanitizer.plain(req.bio())));
        }
        if (req.location() != null) {
            user.setLocation(profanity.clean(TextSanitizer.line(req.location())));
        }
        if (req.website() != null) {
            user.setWebsite(normaliseWebsite(req.website()));
        }
        return userMapper.toMe(user);
    }

    @Transactional
    public MeResponse uploadCover(UUID userId, MultipartFile file) {
        User user = load(userId);
        user.setCoverUrl(storage.uploadImage(file, ImagePreset.COVER));
        return userMapper.toMe(user);
    }

    @Transactional
    public MeResponse removeCover(UUID userId) {
        User user = load(userId);
        user.setCoverUrl(null);
        return userMapper.toMe(user);
    }

    @Transactional(readOnly = true)
    public List<com.connecthub.user.dto.UserSummary> contacts(UUID userId, String q) {
        String query = q == null ? "" : q.trim().replace("%", "").replace("_", "\\_");
        java.util.Set<UUID> hidden = blocks.hiddenFrom(userId);
        return users.findCircleMates(userId, query, 30).stream().filter(u -> !hidden.contains(u.getId()))
                .limit(20).map(circleMapper::toUserSummary).toList();
    }

    /** Owner, public profiles, or anyone who shares an active circle with the owner. */
    public boolean canSeeFull(User owner, UUID viewerId) {
        if (owner.getId().equals(viewerId) || !owner.isPrivateProfile()) {
            return true;
        }
        return viewerId != null && members.shareActiveCircle(owner.getId(), viewerId);
    }

    /** Like {@link #loadByUsername} but 404s when either side has blocked the other. */
    public User loadVisible(String username, UUID viewerId) {
        User user = loadByUsername(username);
        if (blocks.blockedEitherWay(viewerId, user.getId())) {
            throw new NotFoundException("No one goes by that name here");
        }
        return user;
    }

    public User loadByUsername(String username) {
        return users.findByUsername(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("No one goes by that name here"));
    }

    private User load(UUID id) {
        return users.findWithInterestsById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    /** "example.com/me" → "https://example.com/me". Only http(s) URLs with a host are accepted. */
    static String normaliseWebsite(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (!s.matches("(?i)^https?://.*")) {
            s = "https://" + s;
        }
        try {
            URI uri = new URI(s);
            if (uri.getHost() == null || !uri.getHost().contains(".") || s.length() > 255) {
                throw new BadRequestException("Enter a valid website address");
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new BadRequestException("Enter a valid website address");
        }
    }
}
