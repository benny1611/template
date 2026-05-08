package com.benny1611.template.service;

import com.benny1611.template.dao.PasswordResetTokenRepository;
import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dao.UserStateRepository;
import com.benny1611.template.entity.PasswordResetToken;
import com.benny1611.template.entity.User;
import com.benny1611.template.entity.UserState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserStateRepository userStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final IMailService mailService;
    @Value("${app.password.reset.token.expiry-minutes}")
    private int expiryMinutes;
    @Value("${app.password.reset.request.min-duration-ms}")
    private long minDurationMillis;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                UserStateRepository userStateRepository,
                                @Qualifier("sCryptPasswordEncoder") PasswordEncoder passwordEncoder,
                                IMailService mailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userStateRepository = userStateRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    @Transactional
    public void requestReset(String email) {

        long start = System.currentTimeMillis();
        Optional<User> userOptional = userRepository.findByEmail(email);
        String secret = UUID.randomUUID() + "-" + UUID.randomUUID();

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            tokenRepository.invalidateActiveTokens(user, Instant.now());

            PasswordResetToken token = new PasswordResetToken();
            token.setTokenHash(passwordEncoder.encode(secret));
            token.setExpiresAt(
                    Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES)
            );
            token.setUser(user);
            tokenRepository.save(token);

            UUID tokenId = token.getId();

            mailService.sendPasswordResetEmail(user, tokenId, secret, expiryMinutes);
        } else {
            // fake work
            passwordEncoder.encode(secret);
        }
        // Always succeed

        enforceMinimumDuration(start);
    }

    private void enforceMinimumDuration(long startMillis) {
        long elapsed = System.currentTimeMillis() - startMillis;
        long remaining = minDurationMillis - elapsed;

        if (remaining > 0) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Transactional
    public void resetPassword(UUID tokenId, String secret, String newPassword) {
        PasswordResetToken token = tokenRepository
                .findForUpdate(tokenId, Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
        boolean isTokenMatching = passwordEncoder.matches(secret, token.getTokenHash());
        if (!isTokenMatching) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }

        UserState activeState = userStateRepository.findByName("ACTIVE").orElseThrow(() -> new RuntimeException("Could not find the ACTIVE state"));

        User user = token.getUser();
        user.setState(activeState);
        user.setFailedLoginAttempts(0);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }
}
