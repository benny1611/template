package com.benny1611.template.config;

import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dao.UserStateRepository;
import com.benny1611.template.service.LoginService;
import com.benny1611.template.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

@Configuration
public class LoginConfig {

    @Bean
    LoginService loginService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            UserStateRepository userStateRepository,
            @Value("${app.max-failed-password-attempts}") int maxFailedPWAttempts
            ) {
        return new LoginService(userRepository, authenticationManager, jwtUtils, userStateRepository, maxFailedPWAttempts);
    }
}
