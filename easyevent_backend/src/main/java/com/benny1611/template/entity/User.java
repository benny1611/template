package com.benny1611.template.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@FilterDef(
        name = "deletedUserFilter",
        defaultCondition = "deleted_at IS NULL"
)
@Filter(name = "deletedUserFilter")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "language", length = 5)
    private String language;

    @Column(name = "activation_token")
    private UUID activationToken;

    @Column(name = "activation_sent_at")
    private OffsetDateTime activationSentAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private UserState state;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserOAuthAccount> oAuthAccounts;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }

    public User() {}
}
