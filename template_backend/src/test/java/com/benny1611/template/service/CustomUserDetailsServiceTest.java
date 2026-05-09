package com.benny1611.template.service;

import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.entity.Role;
import com.benny1611.template.entity.User;
import com.benny1611.template.exception.AccountSoftDeletedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private final String EMAIL = "test@example.com";

    @Test
    @DisplayName("loadUserByUsername - Success (Active User)")
    void loadUserByUsername_ActiveUser_ReturnsUserDetails() {
        // Arrange
        User user = new User();
        user.setEmail(EMAIL);
        user.setPassword("hashed-password");

        Role role = new Role();
        role.setName("ROLE_USER");
        user.setRoles(Set.of(role));

        when(userRepository.findActiveByEmail(EMAIL)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        // Assert
        assertNotNull(userDetails);
        assertEquals(EMAIL, userDetails.getUsername());
        assertEquals("hashed-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        // Ensure it didn't bother checking for deleted users
        verify(userRepository, never()).findSoftDeletedByEmail(anyString());
    }

    @Test
    @DisplayName("loadUserByUsername - Throws AccountSoftDeletedException for deleted users")
    void loadUserByUsername_SoftDeletedUser_ThrowsException() {
        // Arrange
        User deletedUser = new User();
        deletedUser.setEmail(EMAIL);

        when(userRepository.findActiveByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findSoftDeletedByEmail(EMAIL)).thenReturn(Optional.of(deletedUser));

        // Act & Assert
        assertThrows(AccountSoftDeletedException.class, () -> {
            userDetailsService.loadUserByUsername(EMAIL);
        });
    }

    @Test
    @DisplayName("loadUserByUsername - Throws UsernameNotFoundException when user doesn't exist")
    void loadUserByUsername_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findActiveByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findSoftDeletedByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(EMAIL);
        });

        assertTrue(exception.getMessage().contains(EMAIL));
    }
}
