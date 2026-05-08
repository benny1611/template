package com.benny1611.template.dao;

import com.benny1611.template.entity.UserDeletionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface DeletionLogRepository extends JpaRepository<UserDeletionLog, Long> {

    @Modifying
    @Query("DELETE FROM UserDeletionLog l WHERE l.occurredAt <= :threshold")
    void purgeOldLogs(@Param("threshold") OffsetDateTime threshold);
}
