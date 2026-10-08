package com.connecthub.user;

import com.connecthub.auth.AuthService;
import com.connecthub.auth.RefreshTokenService;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.circle.CircleService;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ConflictException;
import com.connecthub.common.exception.NotFoundException;
import com.connecthub.common.exception.UnauthorizedException;
import com.connecthub.user.dto.ChangeEmailRequest;
import com.connecthub.user.dto.ChangePasswordRequest;
import com.connecthub.user.dto.DeleteAccountRequest;
import com.connecthub.user.dto.MeResponse;
import com.connecthub.user.dto.SettingsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Account settings: privacy, notification preferences, email, password, deletion. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final AuthService authService;
    private final CircleService circles;
    private final CircleMemberRepository members;
    private final UserMapper mapper;

    @Transactional
    public MeResponse updateSettings(UUID userId, SettingsRequest req) {
        User user = load(userId);
        if (req.privateProfile() != null) {
            user.setPrivateProfile(req.privateProfile());
        }
        if (req.notificationPrefs() != null) {
            Map<String, Boolean> prefs = new HashMap<>(user.getNotificationPrefs());
            req.notificationPrefs().forEach((key, value) -> {
                if (!User.DEFAULT_NOTIFICATION_PREFS.containsKey(key) || value == null) {
                    throw new BadRequestException("Unknown notification setting: " + key);
                }
                prefs.put(key, value);
            });
            user.setNotificationPrefs(prefs);
        }
        return mapper.toMe(user);
    }

    @Transactional
    public MeResponse changeEmail(UUID userId, ChangeEmailRequest req) {
        User user = load(userId);
        if (!user.hasPassword()) {
            throw new BadRequestException("Set a password first — your email comes from Google sign-in");
        }
        requirePassword(user, req.currentPassword());
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equals(user.getEmail()) && users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        user.setEmail(email);
        return mapper.toMe(user);
    }

    /**
     * Changes (or, for Google-only accounts, sets) the password. Every existing session is signed
     * out; the caller receives a fresh session for this device.
     */
    @Transactional
    public AuthService.Session changePassword(UUID userId, ChangePasswordRequest req, String userAgent) {
        User user = load(userId);
        if (user.hasPassword()) {
            requirePassword(user, req.currentPassword());
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokens.revokeAll(user);
        return authService.startSession(user, userAgent);
    }

    /**
     * Deletes the account. Circle memberships are left first so admin duties pass to the
     * longest-standing member (and empty circles close); everything else cascades in the database.
     */
    @Transactional
    public void delete(UUID userId, DeleteAccountRequest req) {
        User user = load(userId);
        if (user.hasPassword()) {
            requirePassword(user, req.password());
        } else if (!"DELETE".equals(req.confirm())) {
            throw new BadRequestException("Type DELETE to confirm");
        }
        for (var membership : members.findActiveByUser(userId)) {
            circles.leave(membership.getCircle().getSlug(), userId);
        }
        users.delete(user);
        log.info("Account {} deleted", userId);
    }

    private void requirePassword(User user, String password) {
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Your current password is incorrect");
        }
    }

    private User load(UUID id) {
        return users.findWithInterestsById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
