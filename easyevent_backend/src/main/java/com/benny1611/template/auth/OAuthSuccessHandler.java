package com.benny1611.template.auth;

import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserOAuthAccount;
import com.benny1611.template.service.OAuthCodeService;
import com.benny1611.template.service.UserOAuthAccountService;
import com.benny1611.template.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final UserOAuthAccountService oAuthAccountService;
    private final OAuthCodeService codeService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuthSuccessHandler(UserService userService, UserOAuthAccountService oAuthAccountService, OAuthCodeService codeService) {
        this.userService = userService;
        this.oAuthAccountService = oAuthAccountService;
        this.codeService = codeService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuthUser = authToken.getPrincipal();

        String providerRegistrationId = authToken.getAuthorizedClientRegistrationId();
        String providerUserId = oAuthUser.getAttribute("sub");
        if (providerUserId == null) {
            throw new IllegalStateException("OAuth provider did not return 'sub'");
        }

        String email = oAuthUser.getAttribute("email");
        String name = oAuthUser.getAttribute("name");
        String pictureUrl = oAuthUser.getAttribute("picture");

        Optional<UserOAuthAccount> existingAccount =
                oAuthAccountService.findByProviderAndProviderUserId(
                        providerRegistrationId,
                        providerUserId
                );
        User user;
        boolean isUserBanned = false;
        if (existingAccount.isPresent()) {
            user = existingAccount.get().getUser();
            isUserBanned = oAuthAccountService.isUserBanned(user);
        } else {
            if (email != null) {
                user = userService.findByEmail(email).orElseGet(() -> {
                    try {
                        return userService.createUser(email, name, pictureUrl);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                try {
                    user = userService.createUser(null, name, pictureUrl);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            oAuthAccountService.linkAccount(user, providerRegistrationId, providerUserId, email);
        }

        if (isUserBanned) {
            response.sendRedirect(frontendUrl);
        } else {
            String code = codeService.create(user);

            response.sendRedirect(frontendUrl + "/oauth2/callback?code=" + code);
        }
    }
}
