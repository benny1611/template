package com.benny1611.template.service;

import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dao.UserStateRepository;
import com.benny1611.template.dto.LoginRequest;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserState;
import com.benny1611.template.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class LoginService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserStateRepository userStateRepository;
    private final int maxFailedPWAttempts;

    // @Value("${app.max-failed-password-attempts}")
    @Autowired
    public LoginService(UserRepository userRepository,
                        AuthenticationManager authenticationManager,
                        JwtUtils jwtUtils,
                        UserStateRepository userStateRepository,
                        int maxFailedPWAttempts) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userStateRepository = userStateRepository;
        this.maxFailedPWAttempts = maxFailedPWAttempts;
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public String login(LoginRequest request) {
        Authentication authentication;
        Optional<User> userOptional = userRepository.findByEmailWithRolesAndState(request.getEmail());
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            return null;
        }

        // Check if the user is banned
        UserState bannedState = userStateRepository.findByName("BANNED").orElseThrow(() -> new RuntimeException("Could not find the BANNED state"));
        UserState userState = user.getState();
        if (Objects.equals(userState.getId(), bannedState.getId())) {
            return null;
        }

        UserState blockedState = userStateRepository.findByName("BLOCKED").orElseThrow(() -> new RuntimeException("Could not find the BANNED state"));
        if (Objects.equals(userState.getId(), blockedState.getId())) {
            return null;
        }

        // Check the credentials
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            int failedAttempts = user.getFailedLoginAttempts();
            if (failedAttempts >= maxFailedPWAttempts) {
                user.setState(blockedState);
            }
            userRepository.save(user);
            throw ex;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtils.generateToken(user);
        OffsetDateTime now = OffsetDateTime.now();
        user.setLastLoginAt(now);
        userRepository.save(user);

        return token;
    }
}
