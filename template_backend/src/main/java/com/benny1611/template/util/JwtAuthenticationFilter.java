package com.benny1611.template.util;

import com.benny1611.template.auth.AuthenticatedUser;
import com.benny1611.template.dao.RoleRepository;
import com.benny1611.template.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, RoleRepository roleRepository) {
        this.jwtUtils = jwtUtils;
        this.roleRepository = roleRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/users/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String hearder = request.getHeader("Authorization");

        if (hearder != null && hearder.startsWith("Bearer ")) {
            String token = hearder.substring(7);

            if (jwtUtils.validateToken(token)) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                String email = jwtUtils.getEmailFromToken(token);

                List<Role> allRoles = roleRepository.findAll();
                List<GrantedAuthority> authorities = jwtUtils.getAuthorities(token, allRoles);

                AuthenticatedUser principal = new AuthenticatedUser(userId, email, authorities);
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);

    }
}
