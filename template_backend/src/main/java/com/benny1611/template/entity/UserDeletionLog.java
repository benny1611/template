package com.benny1611.template.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_deletion_log")
@Data
public class UserDeletionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "deletion_type")
    private String deletionType;

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false, nullable = false)
    private Instant occurredAt;

}
