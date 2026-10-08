package com.connecthub.user;

import com.connecthub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interests")
public class Interest extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String slug;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 8)
    private String emoji;
}
