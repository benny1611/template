package com.benny1611.template.cron;

import com.benny1611.template.dao.DeletionLogRepository;
import com.benny1611.template.dao.UserRecoveryLogRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private DeletionLogRepository logRepository;
    @Mock private UserRecoveryLogRepository recoveryLogRepository;

    @InjectMocks
    private CleanupScheduler cleanupScheduler;

    @BeforeEach
    void setUp() {
        // Manually inject the @Value properties
        ReflectionTestUtils.setField(cleanupScheduler, "userPurgeDays", 30L);
        ReflectionTestUtils.setField(cleanupScheduler, "logPurgeDays", 180L);
    }

    @Test
    @DisplayName("runCleanups - Should purge users and logs when expired data exists")
    void runCleanups_Success() {
        // Arrange
        User expiredUser = new User();
        when(userRepository.findExpiredSoftDeletedUsers(any(Instant.class)))
                .thenReturn(List.of(expiredUser));

        // Act
        cleanupScheduler.runCleanups();

        // Assert
        // Verify user deletion called because list was not empty
        verify(userRepository).deleteAll(anyList());

        // Verify log purging called
        verify(logRepository).purgeOldLogs(any(Instant.class));
        verify(recoveryLogRepository).purgeOldLogs(any(Instant.class));
    }

    @Test
    @DisplayName("runCleanups - Should skip user deletion if no expired users found")
    void runCleanups_NoUsersFound() {
        // Arrange
        when(userRepository.findExpiredSoftDeletedUsers(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // Act
        cleanupScheduler.runCleanups();

        // Assert
        // deleteAll should NOT be called for an empty list
        verify(userRepository, never()).deleteAll(anyList());

        // Logs are independent of users, so they should still be purged
        verify(logRepository).purgeOldLogs(any(Instant.class));
        verify(recoveryLogRepository).purgeOldLogs(any(Instant.class));
    }
}