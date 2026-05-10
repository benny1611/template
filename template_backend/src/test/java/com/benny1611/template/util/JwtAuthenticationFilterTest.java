package com.benny1611.template.util;

import com.benny1611.template.auth.AuthenticatedUser;
import com.benny1611.template.dao.RoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext(); // Ensure a clean slate
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should skip filtering for /users/ endpoints")
    void shouldSkipFilteringForUsersEndpoints() {
        request.setRequestURI("/users/login");
        // We call the protected method directly for a unit test
        boolean shouldNotFilter = filter.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("Should authenticate when a valid token is provided")
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        // Arrange
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtUtils.getEmailFromToken(token)).thenReturn("user@example.com");
        when(jwtUtils.getAuthorities(eq(token))).thenReturn(List.of());

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).getEmail()).isEqualTo("user@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate if Authorization header is missing")
    void shouldNotAuthenticateIfHeaderMissing() throws ServletException, IOException {
        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("Should NOT authenticate if token is invalid")
    void shouldNotAuthenticateIfTokenInvalid() throws ServletException, IOException {
        // Arrange
        String token = "invalid-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
