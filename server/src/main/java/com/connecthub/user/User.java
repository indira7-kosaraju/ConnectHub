package com.connecthub.user;

import com.connecthub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    public static final Map<String, Boolean> DEFAULT_NOTIFICATION_PREFS = Map.of(
            "reactions", true, "comments", true, "replies", true, "circles", true, "messages", true);

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "auth_provider")
    private AuthProvider provider = AuthProvider.local;

    @Column(name = "provider_id")
    private String providerId;

    @Column(length = 30)
    private String username;

    @Column(name = "display_name", length = 60)
    private String displayName;

    @Column(length = 160)
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Column(length = 80)
    private String location;

    @Column(length = 255)
    private String website;

    @Column(name = "is_private", nullable = false)
    private boolean privateProfile;

    @Column(nullable = false)
    private boolean onboarded;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_prefs", nullable = false, columnDefinition = "jsonb")
    private Map<String, Boolean> notificationPrefs = new HashMap<>(DEFAULT_NOTIFICATION_PREFS);

    @ManyToMany
    @JoinTable(name = "user_interests",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();

    public boolean hasPassword() {
        return passwordHash != null;
    }
}
