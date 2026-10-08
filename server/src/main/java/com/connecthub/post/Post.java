package com.connecthub.post;

import com.connecthub.circle.Circle;
import com.connecthub.common.BaseEntity;
import com.connecthub.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    public static final int MAX_LENGTH = 500;
    public static final int MAX_IMAGES = 4;
    public static final Duration EDIT_WINDOW = Duration.ofMinutes(15);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @Column(length = MAX_LENGTH)
    private String content;

    @Column(name = "edited_at")
    private Instant editedAt;

    /** Maintained with atomic UPDATEs (see PostRepository); never written through the entity. */
    @Column(name = "reaction_count", nullable = false, insertable = false, updatable = false)
    private int reactionCount;

    @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
    private int commentCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PostImage> images = new ArrayList<>();

    public Instant editableUntil() {
        return getCreatedAt() == null ? null : getCreatedAt().plus(EDIT_WINDOW);
    }

    public boolean isEditable(Instant now) {
        Instant until = editableUntil();
        return until != null && now.isBefore(until);
    }
}
