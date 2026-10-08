package com.connecthub.user;

import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ConflictException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.storage.ImagePreset;
import com.connecthub.storage.StorageService;
import com.connecthub.user.dto.MeResponse;
import com.connecthub.user.dto.UsernameAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** The signed-in user's account: username, avatar, interests and onboarding completion. */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    public static final int MIN_INTERESTS = 3;
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,30}$");
    /** Route names and impersonation-prone handles that can't be claimed. */
    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "root", "api", "app", "auth", "login", "logout", "register", "signup", "signin",
            "settings", "onboarding", "discover", "messages", "notifications", "saved", "search", "explore", "help",
            "support", "about", "terms", "privacy", "connecthub", "official", "staff", "moderator", "system", "null",
            "undefined", "me", "u", "c", "circles", "rewind", "moments");

    private final UserRepository users;
    private final InterestRepository interests;
    private final StorageService storage;
    private final UserMapper mapper;
    private final com.connecthub.moderation.ProfanityFilter profanity;

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        return mapper.toMe(load(userId));
    }

    @Transactional(readOnly = true)
    public UsernameAvailability checkUsername(String raw, UUID currentUserId) {
        String username = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(username).matches()) {
            return new UsernameAvailability(username, false, "INVALID");
        }
        if (RESERVED.contains(username) || profanity.isProfane(username.replace('_', ' '))) {
            return new UsernameAvailability(username, false, "RESERVED");
        }
        boolean takenBySomeoneElse = users.findByUsername(username)
                .filter(u -> !u.getId().equals(currentUserId))
                .isPresent();
        return new UsernameAvailability(username, !takenBySomeoneElse, takenBySomeoneElse ? "TAKEN" : null);
    }

    @Transactional
    public MeResponse setUsername(UUID userId, String raw) {
        UsernameAvailability check = checkUsername(raw, userId);
        if (!check.available()) {
            if ("TAKEN".equals(check.reason())) {
                throw new ConflictException("That username is taken");
            }
            throw new BadRequestException("That username isn't available");
        }
        User user = load(userId);
        user.setUsername(check.username());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That username is taken"); // lost a race with another sign-up
        }
        return mapper.toMe(user);
    }

    @Transactional
    public MeResponse setInterests(UUID userId, Set<UUID> interestIds) {
        List<Interest> found = interests.findAllById(interestIds);
        if (found.size() != interestIds.size()) {
            throw new BadRequestException("Unknown interest");
        }
        User user = load(userId);
        user.setInterests(new HashSet<>(found));
        return mapper.toMe(user);
    }

    @Transactional
    public MeResponse setAvatar(UUID userId, MultipartFile file) {
        String url = storage.uploadImage(file, ImagePreset.AVATAR);
        User user = load(userId);
        user.setAvatarUrl(url);
        return mapper.toMe(user);
    }

    @Transactional
    public MeResponse complete(UUID userId) {
        User user = load(userId);
        if (user.getUsername() == null) {
            throw new BadRequestException("Choose a username first");
        }
        if (user.getInterests().size() < MIN_INTERESTS) {
            throw new BadRequestException("Pick at least " + MIN_INTERESTS + " interests");
        }
        user.setOnboarded(true);
        return mapper.toMe(user);
    }

    private User load(UUID userId) {
        return users.findWithInterestsById(userId).orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
