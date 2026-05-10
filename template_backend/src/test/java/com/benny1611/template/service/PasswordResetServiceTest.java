package com.benny1611.template.service;

import com.benny1611.template.dao.PasswordResetTokenRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dao.UserStateRepository;
import com.benny1611.template.entity.PasswordResetToken;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserStateRepository userStateRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IMailService mailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        // Set @Value fields
        ReflectionTestUtils.setField(passwordResetService, "expiryMinutes", 15);
        ReflectionTestUtils.setField(passwordResetService, "minDurationMillis", 10L); // Keep it fast
    }

    // --- requestReset Tests ---

    @Test
    @DisplayName("requestReset - User exists: Should invalidate old tokens and send email")
    void requestReset_UserExists_Success() {
        // Arrange
        String email = "benny@example.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_secret");

        // Act
        passwordResetService.requestReset(email);

        // Assert
        verify(tokenRepository).invalidateActiveTokens(eq(user), any(Instant.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordResetEmail(eq(user), any(), anyString(), eq(15));
    }

    @Test
    @DisplayName("requestReset - User missing: Should perform fake work and not send email")
    void requestReset_UserMissing_SecurityCheck() {
        // Arrange
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        passwordResetService.requestReset(email);

        // Assert
        // Verify fake work: passwordEncoder should still be called to prevent timing attacks
        verify(passwordEncoder).encode(anyString());
        // Verify no sensitive actions happened
        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
    }

    // --- resetPassword Tests ---

    @Test
    @DisplayName("resetPassword - Success: Should update password and set user to ACTIVE")
    void resetPassword_Success() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        String secret = "raw_secret";
        String newPassword = "NewPassword123!";

        User user = new User();
        user.setPassword("old_hash");

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("hashed_secret");

        UserState activeState = new UserState();
        activeState.setName("ACTIVE");

        when(tokenRepository.findForUpdate(eq(tokenId), any(Instant.class))).thenReturn(Optional.of(token));
        when(passwordEncoder.matches(secret, "hashed_secret")).thenReturn(true);
        when(userStateRepository.findByName("ACTIVE")).thenReturn(Optional.of(activeState));
        when(passwordEncoder.encode(newPassword)).thenReturn("new_hash");

        // Act
        passwordResetService.resetPassword(tokenId, secret, newPassword);

        // Assert
        assertEquals("new_hash", user.getPassword());
        assertEquals(activeState, user.getState());
        assertEquals(0, user.getFailedLoginAttempts());
        assertTrue(token.isUsed());

        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("resetPassword - Invalid Secret: Should throw 400")
    void resetPassword_WrongSecret_ThrowsException() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash("hashed_secret");

        when(tokenRepository.findForUpdate(eq(tokenId), any(Instant.class))).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("wrong_secret", "hashed_secret")).thenReturn(false);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                passwordResetService.resetPassword(tokenId, "wrong_secret", "Pass123!")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid token"));
    }

    @Test
    @DisplayName("resetPassword - Token Expired or Not Found: Should throw 400")
    void resetPassword_TokenNotFound_ThrowsException() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        when(tokenRepository.findForUpdate(eq(tokenId), any(Instant.class))).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                passwordResetService.resetPassword(tokenId, "secret", "Pass123!")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}