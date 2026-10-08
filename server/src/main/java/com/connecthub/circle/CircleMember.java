package com.connecthub.circle;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "circle_members")
public class CircleMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "circle_role")
    private CircleRole role = CircleRole.member;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "member_status")
    private MemberStatus status = MemberStatus.active;

    public CircleMember(Circle circle, User user, CircleRole role, MemberStatus status) {
        this.circle = circle;
        this.user = user;
        this.role = role;
        this.status = status;
    }

    public boolean isActive() {
        return status == MemberStatus.active;
    }

    public boolean isAdmin() {
        return isActive() && role == CircleRole.admin;
    }
}
