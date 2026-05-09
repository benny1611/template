package com.benny1611.template.controller;

import com.benny1611.template.auth.OAuthSuccessHandler;
import com.benny1611.template.config.SecurityConfig;
import com.benny1611.template.dao.RoleRepository;
import com.benny1611.template.dto.OauthCodeRequest;
import com.benny1611.template.service.CustomUserDetailsService;
import com.benny1611.template.service.OAuthService;
import com.benny1611.template.util.JwtUtils;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class OAuthControllerTest {


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthService oAuthService;

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

    @Test
    @DisplayName("POST /exchange - Success (200 OK)")
    void exchange_ShouldReturnToken_WhenRequestIsValid() throws Exception {
        // Arrange
        OauthCodeRequest request = new OauthCodeRequest("valid-code");
        String mockToken = "mocked-jwt-token";

        when(oAuthService.exchange(any(OauthCodeRequest.class))).thenReturn(mockToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // Send the JSON body
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(mockToken));
    }

    @Test
    @DisplayName("POST /exchange - Validation Failure (400 Bad Request)")
    void exchange_ShouldReturnBadRequest_WhenInputIsInvalid() throws Exception {
        // Arrange
        OauthCodeRequest invalidRequest = new OauthCodeRequest(""); // Empty code triggers @NotBlank

        // Act & Assert
        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
