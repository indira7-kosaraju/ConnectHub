package com.connecthub.auth;

import com.connecthub.auth.dto.AuthResponse;
import com.connecthub.auth.dto.LoginRequest;
import com.connecthub.auth.dto.RegisterRequest;
import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.BadRequestException;
import com.connecthub.common.exception.ConflictException;
import com.connecthub.common.exception.UnauthorizedException;
import com.connecthub.user.AuthProvider;
import com.connecthub.user.User;
import com.connecthub.user.UserMapper;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Issued tokens: the access token goes in the body, the refresh token in a cookie. */
    public record Session(AuthResponse body, String refreshToken) {
    }

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final UserMapper userMapper;
    private final com.connecthub.moderation.ProfanityFilter profanity;

    /** Pre-computed hash so failed logins for unknown emails take as long as real ones. */
    private volatile String dummyHash;

    @Transactional
    public Session register(RegisterRequest req, String userAgent) {
        String email = normaliseEmail(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        String displayName = TextSanitizer.line(req.displayName());
        if (displayName == null) {
            throw new BadRequestException("Display name is required");
        }
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(profanity.clean(displayName));
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setProvider(AuthProvider.local);
        users.save(user);
        return startSession(user, userAgent);
    }

    @Transactional
    public Session login(LoginRequest req, String userAgent) {
        User user = users.findByEmailIgnoreCase(normaliseEmail(req.email())).orElse(null);
        if (user == null || !user.hasPassword()) {
            passwordEncoder.matches(req.password(), dummyHash());
            throw new UnauthorizedException(user == null
                    ? "Email or password is incorrect"
                    : "This account uses Google sign-in");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email or password is incorrect");
        }
        return startSession(user, userAgent);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Session refresh(String rawRefreshToken, String userAgent) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken, userAgent);
        User user = users.findWithInterestsById(rotation.user().getId())
                .orElseThrow(() -> new UnauthorizedException("Session expired"));
        return new Session(response(user), rotation.newRawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    /** Creates a session for an already-authenticated user (e.g. after Google sign-in). */
    @Transactional
    public Session startSession(User user, String userAgent) {
        String refresh = refreshTokens.issue(user, userAgent);
        return new Session(response(user), refresh);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.issueAccessToken(user), jwtService.accessTtlSeconds(), userMapper.toMe(user));
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("timing-equaliser");
        }
        return dummyHash;
    }

    static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
