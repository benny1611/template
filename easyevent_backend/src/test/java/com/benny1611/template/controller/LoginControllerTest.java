package com.benny1611.template.controller;

import com.benny1611.template.dao.RoleRepository;
import com.benny1611.template.dto.LoginRequest;
import com.benny1611.template.service.LoginService;
import com.benny1611.template.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LoginController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RoleRepository roleRepository;

    @Test
    void loginSuccess() throws Exception {
        LoginRequest mockRequest = new LoginRequest();
        mockRequest.setEmail("test@email.com");
        mockRequest.setPassword("password");

        when(loginService.login(mockRequest)).thenReturn("access-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "test@email.com",
                            "password": "password"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"));
    }

    @Test
    void loginFailure() throws Exception {
        LoginRequest mockRequest = new LoginRequest();
        mockRequest.setEmail("test@email.com");
        mockRequest.setPassword("password");

        when(loginService.login(mockRequest)).thenReturn(null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "test@email.com",
                            "password": "password"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }
}
