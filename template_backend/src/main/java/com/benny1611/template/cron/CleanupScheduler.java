package com.benny1611.template.cron;

import com.benny1611.template.dao.DeletionLogRepository;
import com.benny1611.template.dao.UserRecoveryLogRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CleanupScheduler {
    private final UserRepository userRepository;
    private final DeletionLogRepository logRepository;
    private final UserRecoveryLogRepository recoveryLogRepository;

    @Value("${app.cleanup.users.purge-days:30}")
    private long userPurgeDays;

    @Value("${app.cleanup.logs.purge-days:180}")
    private long logPurgeDays;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runCleanups() {
        // 1. Hard-delete users
        Instant userThreshold = Instant.now().minus(userPurgeDays, ChronoUnit.DAYS);
        List<User> expiredUsers = userRepository.findExpiredSoftDeletedUsers(userThreshold);

        if (!expiredUsers.isEmpty()) {
            userRepository.deleteAll(expiredUsers);
            log.info("Purged {} expired user accounts.", expiredUsers.size());
        }

        // 2. Clear audit logs
        Instant logThreshold = Instant.now().minus(logPurgeDays, ChronoUnit.DAYS);

        logRepository.purgeOldLogs(logThreshold);
        recoveryLogRepository.purgeOldLogs(logThreshold);

        log.info("Purged all lifecycle logs older than {} days.", logPurgeDays);
    }
}
