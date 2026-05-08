package com.benny1611.template.cron;

import com.benny1611.template.dao.DeletionLogRepository;
import com.benny1611.template.dao.UserRecoveryLogRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CleanupScheduler {
    private final UserRepository userRepository;
    private final DeletionLogRepository logRepository;
    private final UserRecoveryLogRepository recoveryLogRepository;

    // Every night at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runCleanups() {
        // 1. Hard-delete users after 30 days
        OffsetDateTime userThreshold = OffsetDateTime.now().minusDays(30);
        List<User> expiredUsers = userRepository.findExpiredSoftDeletedUsers(userThreshold);
        if (!expiredUsers.isEmpty()) {
            userRepository.deleteAll(expiredUsers);
            log.info("Purged {} expired user accounts.", expiredUsers.size());
        }

        // 2. Clear audit logs after 6 months (~180 days)
        OffsetDateTime logThreshold = OffsetDateTime.now().minusDays(180); // 6 months

        // Purge Deletion Logs
        logRepository.purgeOldLogs(logThreshold);

        // Purge Recovery Logs
        recoveryLogRepository.purgeOldLogs(logThreshold);

        log.info("Purged all lifecycle logs older than 6 months.");
    }
}
