package com.benny1611.template.service;

import com.benny1611.template.dao.OauthProviderRepository;
import com.benny1611.template.dao.UserOAuthAccountRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.entity.OauthProvider;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserOAuthAccount;
import com.benny1611.template.entity.UserState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserOAuthAccountService {

    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final OauthProviderRepository oauthProviderRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserOAuthAccountService(UserOAuthAccountRepository userOAuthAccountRepository, OauthProviderRepository oauthProviderRepository, UserRepository userRepository) {
        this.userOAuthAccountRepository = userOAuthAccountRepository;
        this.oauthProviderRepository = oauthProviderRepository;
        this.userRepository = userRepository;
    }

    public Optional<UserOAuthAccount> findByProviderAndProviderUserId(String providerRegistrationId, String providerUserId) {
        return userOAuthAccountRepository.findByProvider_NameIgnoreCaseAndProviderUserId(providerRegistrationId.toUpperCase(), providerUserId);
    }

    public boolean isUserBanned(User user) {
        Optional<User> userOptional = userRepository.findByIdWithRolesAndState(user.getId());
        if (userOptional.isPresent()) {
            User userWithState = userOptional.get();
            UserState state = userWithState.getState();
            if (state != null) {
                return state.getName().equalsIgnoreCase("BANNED");
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Transactional
    public void linkAccount(User user, String providerRegistrationId, String providerUserId, String email) {
        OauthProvider provider = oauthProviderRepository.findByNameIgnoreCase(providerRegistrationId.toUpperCase()).orElseThrow(() -> new IllegalStateException("Unknown OAuth provider"));
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(user);
        account.setProvider(provider);
        account.setProviderUserId(providerUserId);
        account.setEmail(email);
        account.setConnectedAt(Instant.now());

        userOAuthAccountRepository.save(account);
    }
}
