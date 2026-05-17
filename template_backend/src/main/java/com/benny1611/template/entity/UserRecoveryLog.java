package com.benny1611.template.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_recovery_log")
@Data
public class UserRecoveryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "recovered_by_id", nullable = false)
    private Long recoveredById;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false, nullable = false)
    private Instant occurredAt;
}
