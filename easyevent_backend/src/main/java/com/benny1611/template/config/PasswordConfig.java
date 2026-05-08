package com.benny1611.template.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

@Configuration
public class PasswordConfig {

    @Value("${app.password.bcrypt.strength}")
    private int strength;

    @Bean
    public PasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public PasswordEncoder sCryptPasswordEncoder() {
        return new SCryptPasswordEncoder(65536, 8,1, 32, 16);
    }
}
