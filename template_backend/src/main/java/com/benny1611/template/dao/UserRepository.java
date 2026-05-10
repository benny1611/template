package com.benny1611.template.dao;

import com.benny1611.template.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
    SELECT DISTINCT u
    FROM User u
    LEFT JOIN FETCH u.roles
    WHERE u.email = :email
    """)
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("""
    SELECT DISTINCT u
    FROM User u
    LEFT JOIN FETCH u.roles
    WHERE u.id = :id
    """)
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("""
    SELECT u 
    FROM User u 
    JOIN FETCH u.state 
    LEFT JOIN FETCH u.roles 
    WHERE u.id = :id
    """)
    Optional<User> findByIdWithRolesAndState(@Param("id") Long id);

    @Query("""
    SELECT DISTINCT u
    FROM User u
    JOIN FETCH u.state
    LEFT JOIN FETCH u.roles
    WHERE u.id IN :ids
    """)
    List<User> findAllByIdWithRolesAndState(@Param("ids") List<Long> ids);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles r
        JOIN FETCH u.state s
        WHERE u.email = :email
    """)
    Optional<User> findByEmailWithRolesAndState(@Param("email") String email);

    @Query("SELECT u.id FROM User u")
    Page<Long> findUserIds(Pageable pageable);

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findByActivationToken(@Param("token") UUID token);

    // Recovery: Native query to find "hidden" rows
    @Query(value = "SELECT * FROM users WHERE email = :email AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<User> findSoftDeletedByEmail(@Param("email") String email);

    // For the cleanup task
    @Query(value = "SELECT * FROM users WHERE deleted_at <= :threshold", nativeQuery = true)
    List<User> findExpiredSoftDeletedUsers(@Param("threshold") Instant threshold);

    @Query(value = "SELECT * FROM users WHERE email = :email AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findActiveByEmail(String email);
}
