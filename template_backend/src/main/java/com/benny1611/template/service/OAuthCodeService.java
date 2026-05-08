package com.benny1611.template.service;

import com.benny1611.template.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthCodeService {
    private final Duration TTL;

    private class Entry {
        final Long userId;
        final Instant expiresAt;

        private Entry(Long userId) {
            this.userId = userId;
            this.expiresAt = Instant.now().plus(TTL);
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public OAuthCodeService(@Value("${oauth.code.ttl-s}") int ttlInt) {
        TTL = Duration.ofSeconds(ttlInt);
    }

    public String create(User user) {
        String code = UUID.randomUUID().toString();
        store.put(code, new Entry(user.getId()));
        return code;
    }

    public Long consume(String code) {
        Entry entry = store.remove(code);

        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OAuth code");
        }

        return entry.userId;
    }
}
