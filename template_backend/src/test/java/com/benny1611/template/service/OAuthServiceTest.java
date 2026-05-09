package com.benny1611.template.service;

import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dto.OauthCodeRequest;
import com.benny1611.template.entity.User;
import com.benny1611.template.exception.AccountSoftDeletedException;
import com.benny1611.template.util.JwtUtils;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock private OAuthCodeService codeService;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;
    @Mock private Session session; // We need to mock the Hibernate Session

    @InjectMocks
    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuthService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("Exchange - Success")
    void exchange_Success() {
        // Arrange
        String code = "valid-code";
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setEmail("benny@example.com");

        when(codeService.consume(code)).thenReturn(userId);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(user)).thenReturn("mock-jwt");

        // Act
        String token = oAuthService.exchange(new OauthCodeRequest(code));

        // Assert
        assertEquals("mock-jwt", token);
        assertNotNull(user.getLastLoginAt());
        verify(session).disableFilter("deletedUserFilter");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Exchange - Should throw Exception if user is soft-deleted")
    void exchange_UserDeleted_ThrowsException() {
        // Arrange
        String code = "valid-code";
        Long userId = 1L;
        User user = new User();
        user.setDeletedAt(OffsetDateTime.now()); // Mark as deleted

        when(codeService.consume(code)).thenReturn(userId);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(AccountSoftDeletedException.class, () -> {
            oAuthService.exchange(new OauthCodeRequest(code));
        });
    }
}
