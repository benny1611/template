package com.benny1611.template.util;

import com.benny1611.template.entity.Role;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    // A 256-bit (32-byte) secret encoded in Base64
    private final String SECRET = Base64.getEncoder().encodeToString("very-secret-key-that-is-at-least-32-bytes-long".getBytes());
    private final int DURATION = 1;

    private User testUser;
    private Role roleUser;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(DURATION, SECRET);

        roleUser = new Role();
        roleUser.setName("ROLE_USER");

        UserState state = new UserState();
        state.setName("ACTIVE");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("benny@example.com");
        testUser.setName("Benny");
        testUser.setRoles(Set.of(roleUser));
        testUser.setState(state);
        testUser.setPassword("hashed-password");
    }

    @Test
    @DisplayName("generateToken & validateToken - Should create a valid token and verify it")
    void tokenLifecycle_Success() {
        // Act
        String token = jwtUtils.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("Extraction - Should correctly extract User ID and Email from token")
    void extractClaims_Success() {
        // Arrange
        String token = jwtUtils.generateToken(testUser);

        // Act
        Long userId = jwtUtils.getUserIdFromToken(token);
        String email = jwtUtils.getEmailFromToken(token);

        // Assert
        assertEquals(1L, userId);
        assertEquals("benny@example.com", email);
    }

    @Test
    @DisplayName("getAuthorities - Should return authorities only for roles present in the allowed list")
    void getAuthorities_FiltersCorrectly() {
        // Arrange
        String token = jwtUtils.generateToken(testUser);

        Role roleAdmin = new Role();
        roleAdmin.setName("ROLE_ADMIN");

        // The user has ROLE_USER in the token.
        // We provide a list that includes ROLE_USER and ROLE_ADMIN.
        List<Role> allPossibleRoles = List.of(roleUser, roleAdmin);

        // Act
        List<GrantedAuthority> authorities = jwtUtils.getAuthorities(token, allPossibleRoles);

        // Assert
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0).getAuthority());
    }

    @Test
    @DisplayName("validateToken - Should return false for invalid or tampered tokens")
    void validateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String token = jwtUtils.generateToken(testUser);
        String tamperedToken = token + "modified";

        // Act & Assert
        assertFalse(jwtUtils.validateToken(tamperedToken));
        assertFalse(jwtUtils.validateToken("not.a.token"));
    }

    @Test
    @DisplayName("validateToken - Should return false for token signed with different key")
    void validateToken_DifferentKey_ReturnsFalse() {
        // Arrange
        String differentSecret = Base64.getEncoder().encodeToString("another-completely-different-secret-key-long".getBytes());
        JwtUtils otherUtils = new JwtUtils(1, differentSecret);

        String tokenFromOtherKey = otherUtils.generateToken(testUser);

        // Act & Assert
        assertFalse(jwtUtils.validateToken(tokenFromOtherKey));
    }
}
