package com.benny1611.template.service;

import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dao.UserStateRepository;
import com.benny1611.template.dto.LoginRequest;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserState;
import com.benny1611.template.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtUtils jwtUtils;
    @Mock
    UserStateRepository userStateRepository;

    LoginService loginService;

    UserState blockedState;
    UserState bannedState;
    UserState activeState;

    @BeforeEach
    void setup() {
        loginService = new LoginService(userRepository, authenticationManager, jwtUtils, userStateRepository, 3);

        blockedState = new UserState();
        blockedState.setName("BLOCKED");
        blockedState.setId((short) 3);

        activeState = new UserState();
        activeState.setName("ACTIVE");
        activeState.setId((short) 1);

        bannedState = new UserState();
        bannedState.setName("BANNED");
        bannedState.setId((short) 4);

        when(userStateRepository.findByName("BLOCKED")).thenReturn(Optional.of(blockedState));
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(blockedState));
    }

    @Test
    void loginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@email.com");

        User user = new User();
        user.setEmail("test@email.com");
        user.setState(activeState);

        when(userRepository.findByEmailWithRolesAndState("test@email.com")).thenReturn(Optional.of(user));

        when(jwtUtils.generateToken(user)).thenReturn("token");

        String token = loginService.login(request);
        assertEquals("token", token);
    }

    @Test
    void loginFailure() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@email.com");

        // user does not exist
        AtomicReference<String> token = new AtomicReference<>(loginService.login(request));
        assertNull(token.get());

        User user = new User();
        user.setEmail("test@email.com");
        user.setState(activeState);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByEmailWithRolesAndState("test@email.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException(""));

        assertThrows(AuthenticationException.class, () ->
                loginService.login(request)
        );
        assertEquals(1, user.getFailedLoginAttempts());

        assertThrows(AuthenticationException.class, () ->
                loginService.login(request)
        );
        assertEquals(2, user.getFailedLoginAttempts());

        assertThrows(AuthenticationException.class, () ->
                token.set(loginService.login(request))
        );
        assertNull(token.get());
        assertEquals(3, user.getFailedLoginAttempts());
        assertEquals(blockedState.getId(), user.getState().getId());

        // Banned user
        LoginRequest bannedRequest = new LoginRequest();
        bannedRequest.setEmail("banned@email.com");

        User bannedUser = new User();
        bannedUser.setEmail("banned@email.com");
        bannedUser.setState(bannedState);
        bannedUser.setFailedLoginAttempts(0);

        when(userRepository.findByEmailWithRolesAndState("banned@email.com")).thenReturn(Optional.of(bannedUser));
        assertThrows(AuthenticationException.class, () ->
                loginService.login(bannedRequest)
        );
    }
}
