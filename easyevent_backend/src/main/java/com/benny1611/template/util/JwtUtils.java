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
import java.util.ArrayList;
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

    public List<GrantedAuthority> getAuthorities(String token, List<Role> allRoles) {
        Object rolesObj = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("roles");
        List<String> rolesStringList = List.of();
        if (rolesObj != null) {
            try {
                rolesStringList = (List<String>) rolesObj;
            } catch (ClassCastException e) {
                LOG.error("Could not cast authorities to list: {}", rolesObj);
                return null;
            }
        }
        List<GrantedAuthority> roles = new ArrayList<>();
        List<String> allRolesString = allRoles.stream().map(Role::getName).toList();

        for (String role: rolesStringList) {
            if (allRolesString.contains(role)) {
                roles.add(new SimpleGrantedAuthority(role));
            }
        }

        return roles;
    }
}
