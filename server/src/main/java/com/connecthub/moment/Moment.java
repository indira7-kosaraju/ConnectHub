package com.connecthub.moment;

import com.connecthub.common.BaseEntity;
import com.connecthub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "moments")
public class Moment extends BaseEntity {

    public static final Duration LIFETIME = Duration.ofHours(24);
    public static final int MAX_TEXT = 200;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "moment_kind")
    private MomentKind kind;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(length = MAX_TEXT)
    private String text;

    @Column(length = 20)
    private String background;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean archived;

    /** Maintained atomically by MomentRepository; read-only through the entity. */
    @Column(name = "view_count", nullable = false, insertable = false, updatable = false)
    private int viewCount;

    public boolean isLive(Instant now) {
        return !archived && expiresAt.isAfter(now);
    }
}
