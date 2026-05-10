package com.benny1611.template.auth;

import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserOAuthAccount;
import com.benny1611.template.service.OAuthCodeService;
import com.benny1611.template.service.UserOAuthAccountService;
import com.benny1611.template.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthSuccessHandlerTest {

    @Mock private UserService userService;
    @Mock private UserOAuthAccountService oAuthAccountService;
    @Mock private OAuthCodeService codeService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private OAuth2AuthenticationToken authToken;
    @Mock private OAuth2User oAuthUser;

    @InjectMocks
    private OAuthSuccessHandler handler;

    private final String FRONTEND_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", FRONTEND_URL);
    }

    private void setupOAuthMock(String sub, String email, String name) {
        when(authToken.getPrincipal()).thenReturn(oAuthUser);
        when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oAuthUser.getAttribute("sub")).thenReturn(sub);
        when(oAuthUser.getAttribute("email")).thenReturn(email);
        when(oAuthUser.getAttribute("name")).thenReturn(name);
    }

    @Test
    @DisplayName("Success - Existing account, not banned")
    void onAuthenticationSuccess_ExistingAccount_NotBanned() throws IOException {
        // Arrange
        setupOAuthMock("user-123", "benny@example.com", "Benny");
        User user = new User();
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(user);

        when(oAuthAccountService.findByProviderAndProviderUserId("google", "user-123"))
                .thenReturn(Optional.of(account));
        when(oAuthAccountService.isUserBanned(user)).thenReturn(false);
        when(codeService.create(user)).thenReturn("valid-code");

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        verify(response).sendRedirect(FRONTEND_URL + "/oauth2/callback?code=valid-code");
    }

    @Test
    @DisplayName("Success - Existing account, IS banned")
    void onAuthenticationSuccess_ExistingAccount_Banned() throws IOException {
        // Arrange
        setupOAuthMock("user-123", "benny@example.com", "Benny");
        User user = new User();
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(user);

        when(oAuthAccountService.findByProviderAndProviderUserId("google", "user-123"))
                .thenReturn(Optional.of(account));
        when(oAuthAccountService.isUserBanned(user)).thenReturn(true);

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        // Should redirect to root frontend URL without a code
        verify(response).sendRedirect(FRONTEND_URL);
        verify(codeService, never()).create(any());
    }

    @Test
    @DisplayName("Success - New account, creates user and links")
    void onAuthenticationSuccess_NewUser_CreatesAndLinks() throws IOException {
        // Arrange
        setupOAuthMock("new-sub", "new@example.com", "New User");
        User newUser = new User();

        when(oAuthAccountService.findByProviderAndProviderUserId("google", "new-sub"))
                .thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser("new@example.com", "New User", null)).thenReturn(newUser);
        when(codeService.create(newUser)).thenReturn("new-code");

        // Act
        handler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        verify(userService).createUser("new@example.com", "New User", null);
        verify(oAuthAccountService).linkAccount(newUser, "google", "new-sub", "new@example.com");
        verify(response).sendRedirect(FRONTEND_URL + "/oauth2/callback?code=new-code");
    }

    @Test
    @DisplayName("Failure - Provider missing 'sub' attribute")
    void onAuthenticationSuccess_MissingSub_ThrowsException() {
        // Arrange
        when(authToken.getPrincipal()).thenReturn(oAuthUser);
        when(oAuthUser.getAttribute("sub")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                handler.onAuthenticationSuccess(request, response, authToken)
        );
    }
}
