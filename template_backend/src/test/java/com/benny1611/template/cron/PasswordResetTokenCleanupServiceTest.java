package com.benny1611.template.cron;

import com.benny1611.template.dao.PasswordResetTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenCleanupServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private PasswordResetTokenCleanupService cleanupService;

    @Test
    @DisplayName("cleanupExpiredTokens - Should call repository with current timestamp")
    void cleanupExpiredTokens_ShouldInvokeRepository() {
        // Act
        cleanupService.cleanupExpiredTokens();

        // Assert
        // We verify that the repository method was called.
        // Using any(Instant.class) because Instant.now() is called inside the method.
        verify(tokenRepository).deleteExpiredOrUsed(any(Instant.class));
    }
}
