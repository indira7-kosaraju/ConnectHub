package com.connecthub.circle;

import com.connecthub.common.BaseEntity;
import com.connecthub.user.Interest;
import com.connecthub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "circles")
public class Circle extends BaseEntity {

    public static final int MAX_MEMBERS = 150;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    /** Circles are categorised by the same taxonomy users pick as interests. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Interest category;

    @Column(name = "is_private", nullable = false)
    private boolean privateCircle;

    @Column(name = "max_members", nullable = false)
    private int maxMembers = MAX_MEMBERS;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "pinned_post_id")
    private UUID pinnedPostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public boolean isFull() {
        return memberCount >= maxMembers;
    }
}
