package com.benny1611.template.cron;

import com.benny1611.template.dao.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PasswordResetTokenCleanupService {

    private final PasswordResetTokenRepository tokenRepository;

    @Autowired
    public PasswordResetTokenCleanupService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredOrUsed(Instant.now());
    }
}
