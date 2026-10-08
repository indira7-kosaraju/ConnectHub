package com.connecthub.auth.oauth;

import com.connecthub.common.TextSanitizer;
import com.connecthub.common.exception.UnauthorizedException;
import com.connecthub.user.AuthProvider;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Finds or creates the local user for a Google identity. Existing email/password accounts are
 * linked only when Google asserts the email is verified.
 */
@Service
@RequiredArgsConstructor
public class GoogleAccountService {

    private final UserRepository users;

    @Transactional
    public User upsert(OidcUser google) {
        String subject = google.getSubject();
        var existing = users.findByProviderId(subject);
        if (existing.isPresent()) {
            return existing.get();
        }
        String email = google.getEmail();
        if (email == null || !Boolean.TRUE.equals(google.getEmailVerified())) {
            throw new UnauthorizedException("Your Google account needs a verified email address");
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user != null) {
            user.setProviderId(subject); // link Google to the existing account
            if (user.getAvatarUrl() == null) {
                user.setAvatarUrl(google.getPicture());
            }
            return user;
        }
        user = new User();
        user.setEmail(email);
        user.setProvider(AuthProvider.google);
        user.setProviderId(subject);
        String name = TextSanitizer.line(google.getFullName());
        user.setDisplayName(name != null && name.length() > 60 ? name.substring(0, 60) : name);
        user.setAvatarUrl(google.getPicture());
        return users.save(user);
    }
}
