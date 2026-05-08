package com.benny1611.template.dao;

import com.benny1611.template.entity.PasswordResetToken;
import com.benny1611.template.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t FROM PasswordResetToken t
        WHERE t.id = :id
          AND t.used = false
          AND t.expiresAt > :now
    """)
    Optional<PasswordResetToken> findForUpdate(
            UUID id,
            Instant now
    );

    @Modifying
    @Query("""
            DELETE FROM PasswordResetToken t
            WHERE t.used = true
            OR t.expiresAt < :now
            """)
    void deleteExpiredOrUsed(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
            SET t.used = true
            WHERE t.user = :user
            AND t.used = false
            AND t.expiresAt > :now
            """)
    int invalidateActiveTokens(User user, Instant now);
}
