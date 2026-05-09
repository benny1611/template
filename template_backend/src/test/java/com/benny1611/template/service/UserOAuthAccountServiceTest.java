package com.benny1611.template.service;

import com.benny1611.template.dao.OauthProviderRepository;
import com.benny1611.template.dao.UserOAuthAccountRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.entity.OauthProvider;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserOAuthAccount;
import com.benny1611.template.entity.UserState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserOAuthAccountServiceTest {

    @Mock private UserOAuthAccountRepository userOAuthAccountRepository;
    @Mock private OauthProviderRepository oauthProviderRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserOAuthAccountService userOAuthAccountService;

    @Test
    @DisplayName("findByProviderAndProviderUserId - Should uppercase provider name")
    void findByProvider_ShouldUppercaseProvider() {
        // Arrange
        String provider = "google";
        String userId = "12345";

        // Act
        userOAuthAccountService.findByProviderAndProviderUserId(provider, userId);

        // Assert - Verify the repository is called with "GOOGLE"
        verify(userOAuthAccountRepository).findByProvider_NameIgnoreCaseAndProviderUserId("GOOGLE", userId);
    }

    @Test
    @DisplayName("isUserBanned - Should return true when state is BANNED")
    void isUserBanned_True() {
        // Arrange
        User user = new User();
        user.setId(1L);

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        user.setState(bannedState);

        when(userRepository.findByIdWithRolesAndState(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        assertTrue(userOAuthAccountService.isUserBanned(user));
    }

    @Test
    @DisplayName("isUserBanned - Should return false when state is null or different")
    void isUserBanned_False() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setState(null); // Case 1: No state

        when(userRepository.findByIdWithRolesAndState(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        assertFalse(userOAuthAccountService.isUserBanned(user));

        // Case 2: Different state
        UserState activeState = new UserState();
        activeState.setName("ACTIVE");
        user.setState(activeState);

        assertFalse(userOAuthAccountService.isUserBanned(user));
    }

    @Test
    @DisplayName("linkAccount - Success")
    void linkAccount_Success() {
        // Arrange
        User user = new User();
        OauthProvider provider = new OauthProvider();
        provider.setName("GITHUB");

        when(oauthProviderRepository.findByNameIgnoreCase("GITHUB")).thenReturn(Optional.of(provider));

        // Act
        userOAuthAccountService.linkAccount(user, "github", "sub_123", "test@test.com");

        // Assert
        // We use an ArgumentCaptor to inspect the object that was sent to .save()
        ArgumentCaptor<UserOAuthAccount> accountCaptor = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(userOAuthAccountRepository).save(accountCaptor.capture());

        UserOAuthAccount savedAccount = accountCaptor.getValue();
        assertEquals(user, savedAccount.getUser());
        assertEquals(provider, savedAccount.getProvider());
        assertEquals("sub_123", savedAccount.getProviderUserId());
        assertNotNull(savedAccount.getConnectedAt());
    }

    @Test
    @DisplayName("linkAccount - Throws Exception for unknown provider")
    void linkAccount_UnknownProvider() {
        // Arrange
        when(oauthProviderRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            userOAuthAccountService.linkAccount(new User(), "unknown", "id", "email");
        });
    }
}
