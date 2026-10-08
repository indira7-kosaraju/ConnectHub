package com.connecthub.message;

import com.connecthub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    /** "<smaller uuid>:<larger uuid>" — guarantees one 1:1 conversation per pair. */
    @Column(name = "direct_key", nullable = false, unique = true, length = 73)
    private String directKey;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public static String directKey(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }
}
