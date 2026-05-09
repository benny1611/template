package com.benny1611.template.controller;

import com.benny1611.template.auth.OAuthSuccessHandler;
import com.benny1611.template.config.SecurityConfig;
import com.benny1611.template.dao.RoleRepository;
import com.benny1611.template.dto.PasswordResetConfirmRequest;
import com.benny1611.template.dto.PasswordResetRequest;
import com.benny1611.template.service.CustomUserDetailsService;
import com.benny1611.template.service.PasswordResetService;
import com.benny1611.template.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean(name = "bcryptPasswordEncoder")
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // Manually initializing or autowiring depending on your previous setup
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /request - Success")
    void requestReset_Success() throws Exception {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("benny@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(passwordResetService).requestReset("benny@example.com");
    }

    @Test
    @DisplayName("POST /confirm - Success")
    void confirmReset_Success() throws Exception {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        String secret = "secret-token";
        // Updated to include letters, numbers, and a special character
        String newPassword = "Password123!";

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setTokenId(tokenId);
        request.setSecret(secret);
        request.setNewPassword(newPassword);

        // Act & Assert
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(passwordResetService).resetPassword(tokenId, secret, newPassword);
    }

    @Test
    @DisplayName("POST /request - Validation Failure (Invalid Email)")
    void requestReset_InvalidEmail_Returns400() throws Exception {
        // Arrange: Sending an empty email to trigger @Valid
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /confirm - Failure (Weak Password)")
    void confirmReset_WeakPassword_Returns400() throws Exception {
        // Arrange: Password missing numbers/special chars
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setTokenId(UUID.randomUUID());
        request.setSecret("secret");
        request.setNewPassword("weak");

        // Act & Assert
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
