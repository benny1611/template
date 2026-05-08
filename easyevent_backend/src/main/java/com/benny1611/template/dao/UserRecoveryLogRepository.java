package com.benny1611.template.dao;

import com.benny1611.template.entity.UserRecoveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface UserRecoveryLogRepository extends JpaRepository<UserRecoveryLog, Long> {
    @Modifying
    @Query("DELETE FROM UserRecoveryLog l WHERE l.occurredAt <= :threshold")
    void purgeOldLogs(@Param("threshold") OffsetDateTime threshold);
}
