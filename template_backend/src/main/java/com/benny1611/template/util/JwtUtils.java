package com.benny1611.template.util;

import com.benny1611.template.entity.Role;
import com.benny1611.template.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JwtUtils.class);

    private final SecretKey key;
    private final long expiration;

    public JwtUtils (@Value("${app.security.tokenDurationInHours}") int tokenDurationInHours,
                     @Value("${app.security.jwt-secret}") String secretString) {
        byte[] keyBytes = Decoders.BASE64.decode(secretString);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        expiration = TimeUnit.HOURS.toMillis(tokenDurationInHours);
    }

    public String generateToken(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String password = user.getPassword();
        boolean isLocalPasswordSet = password == null;
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .claim("roles", roles)
                .claim("profilePictureUrl", user.getProfilePictureUrl())
                .claim("email", user.getEmail())
                .claim("username", user.getName())
                .claim("isLocalPasswordSet", isLocalPasswordSet)
                .claim("state", user.getState().getName())
                .signWith(key)
                .compact();
    }


    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    public String getEmailFromToken(String token) {
        Object email = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email");

        return email != null ? email.toString() : null;
    }

    public List<GrantedAuthority> getAuthorities(String token) {
        Object rolesObj = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles");

        if (rolesObj instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new) // No DB lookup needed!
                    .map(GrantedAuthority.class::cast)
                    .toList();
        }

        return List.of();
    }
}
