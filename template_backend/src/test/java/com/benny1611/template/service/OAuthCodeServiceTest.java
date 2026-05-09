package com.benny1611.template.service;

import com.benny1611.template.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class OAuthCodeServiceTest {

    private OAuthCodeService oAuthCodeService;

    @BeforeEach
    void setUp() {
        // We pass the TTL directly to the constructor
        int TTL_SECONDS = 1;
        oAuthCodeService = new OAuthCodeService(TTL_SECONDS);
    }

    @Test
    @DisplayName("Should create and consume code successfully")
    void createAndConsume_Success() {
        User user = new User();
        user.setId(100L);

        String code = oAuthCodeService.create(user);
        assertNotNull(code);

        Long userId = oAuthCodeService.consume(code);
        assertEquals(100L, userId);
    }

    @Test
    @DisplayName("Should throw 401 when code is invalid")
    void consume_InvalidCode_ThrowsException() {
        assertThrows(ResponseStatusException.class, () -> {
            oAuthCodeService.consume("non-existent-code");
        });
    }

    @Test
    @DisplayName("Should throw 401 when code is expired")
    void consume_ExpiredCode_ThrowsException() throws InterruptedException {
        User user = new User();
        user.setId(100L);

        String code = oAuthCodeService.create(user);

        // Wait for TTL to pass
        Thread.sleep(1100);

        assertThrows(ResponseStatusException.class, () -> {
            oAuthCodeService.consume(code);
        });
    }
}