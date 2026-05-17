package com.benny1611.template.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "user_oauth_accounts",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"provider_id", "provider_user_id"}),
            @UniqueConstraint(columnNames = {"user_id", "provider_id"})
        })
@Data
public class UserOAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id")
    private OauthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "email")
    private String email;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;
}
