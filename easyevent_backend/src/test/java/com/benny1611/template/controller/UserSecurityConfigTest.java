package com.benny1611.template.controller;

import com.benny1611.template.auth.OAuthSuccessHandler;
import com.benny1611.template.config.SecurityConfig;
import com.benny1611.template.dto.ListUserResponse;
import com.benny1611.template.dto.UserDTO;
import com.benny1611.template.service.CustomUserDetailsService;
import com.benny1611.template.service.UserService;
import com.benny1611.template.util.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
public class UserSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean(name = "bcryptPasswordEncoder")
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    // update user tests

    @Test
    public void updatingUserWithoutAuthShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/users/update")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void updatingUserWithWrongRoleShouldReturn403() throws Exception {
        performPutRequestToUpdateUsers(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void updatingUserWithRightRoleShouldReturn200() throws Exception {
        performPutRequestToUpdateUsers(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updatingUserWithRightRoleShouldReturn200_ADMIN() throws Exception {
        performPutRequestToUpdateUsers(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void updatingUserWithRightRoleShouldReturn200_SUPER_ADMIN() throws Exception {
        performPutRequestToUpdateUsers(status().isOk());
    }

    private void performPutRequestToUpdateUsers(ResultMatcher expectedResult) throws Exception {
        UserDTO mockUserDTO = new UserDTO();
        when(userService.updateUser(any(), any(), any())).thenReturn(mockUserDTO);
        String jsonForRole = """
    {
        "id": 1
    }
    """;

        MockMultipartFile userPart = new MockMultipartFile(
                "userDTO", "", "application/json", jsonForRole.getBytes()
        );

        mockMvc.perform(multipart("/api/users/update")
                        .file(userPart) // adding dummy user so that the request will go through
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andExpect(expectedResult);
    }

    // update user by admin tests

    @Test
    public void updatingUserByAdminWithoutAuthShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/users/update/1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void updatingUserByAdminWithWrongRoleShouldReturn403() throws Exception {
        performPutRequestToUpdateUsersByAdmin(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void updatingUserByAdminWithWrongRoleShouldReturn403_USER() throws Exception {
        performPutRequestToUpdateUsersByAdmin(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updatingUserByAdminWithRightRoleShouldReturn200_ADMIN() throws Exception {
        performPutRequestToUpdateUsersByAdmin(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void updatingUserByAdminWithRightRoleShouldReturn200_SUPER_ADMIN() throws Exception {
        performPutRequestToUpdateUsersByAdmin(status().isOk());
    }

    // update user by super admin tests

    @Test
    public void updatingUserBySuperAdminWithoutAuthShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/users/update/admin/1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void updatingUserBySuperAdminWithWrongRoleShouldReturn403() throws Exception {
        performPutRequestToUpdateUsersBySuperAdminAdmin(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void updatingUserBySuperAdminWithWrongRoleShouldReturn403_USER() throws Exception {
        performPutRequestToUpdateUsersBySuperAdminAdmin(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updatingUserBySuperAdminWithWrongRoleShouldReturn403_ADMIN() throws Exception {
        performPutRequestToUpdateUsersBySuperAdminAdmin(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void updatingUserBySuperAdminWithRightRoleShouldReturn200_SUPER_ADMIN() throws Exception {
        performPutRequestToUpdateUsersBySuperAdminAdmin(status().isOk());
    }

    private void performPutRequestToUpdateUsersByAdmin(ResultMatcher expectedResult) throws Exception {
        performPutRequestToUpdateUsersByAnAdmin(expectedResult, false);
    }
    private void performPutRequestToUpdateUsersBySuperAdminAdmin(ResultMatcher expectedResult) throws Exception {
        performPutRequestToUpdateUsersByAnAdmin(expectedResult, true);
    }

    private void performPutRequestToUpdateUsersByAnAdmin(ResultMatcher expectedResult, boolean sendToSuperAdmin) throws Exception {
        ListUserResponse mockResponse = new ListUserResponse();
        when(userService.updateUserByAdmin(any(), any(), any(), any())).thenReturn(mockResponse);
        when(userService.updateUserBySuperAdmin(any(), any(), any(), any())).thenReturn(mockResponse);
        String validJsonButWrongRole = """
    {
        "email": "test@test.com"
    }
    """;

        MockMultipartFile userPart = new MockMultipartFile(
                "changeUserRequest", "", "application/json", validJsonButWrongRole.getBytes()
        );
        String url;
        if (sendToSuperAdmin) {
            url = "/api/users/update/admin/1";
        } else {
            url = "/api/users/update/1";
        }

        mockMvc.perform(multipart(url)
                        .file(userPart) // adding dummy user so that the request will go through
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andExpect(expectedResult);
    }


    // Test ban
    @Test
    public void banningUserByIdWithoutAuthShouldReturn401() throws Exception {
        testBan(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void banningUserByIdGuestShouldReturn403() throws Exception {
        testBan(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void banningUserByIdUserShouldReturn403() throws Exception {
        testBan(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void banningUserByIdAdminShouldReturn200() throws Exception {
        testBan(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void banningUserByIdSuperAdminShouldReturn200() throws Exception {
        testBan(status().isOk());
    }

    private void testBan(ResultMatcher expectedResult) throws Exception {
        when(userService.banUserById(any(), any(), any())).thenReturn(true);
        JSONObject banRequest = new JSONObject();
        banRequest.put("reason", "test");
        mockMvc.perform(post("/api/users/ban/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(banRequest.toString())
                        .with(csrf()))
                .andExpect(expectedResult);
    }

    // Test unban
    @Test
    public void unbanningUserByIdWithoutAuthShouldReturn401() throws Exception {
        testUnban(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void unbanningUserByIdGuestShouldReturn403() throws Exception {
        testUnban(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void unbanningUserByIdUserShouldReturn403() throws Exception {
        testUnban(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void unbanningUserByIdAdminShouldReturn200() throws Exception {
        testUnban(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void unbanningUserByIdSuperAdminShouldReturn200() throws Exception {
        testUnban(status().isOk());
    }

    private void testUnban(ResultMatcher expectedResult) throws Exception {
        when(userService.unbanUserById(any(), any())).thenReturn(true);
        mockMvc.perform(post("/api/users/unban/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(expectedResult);
    }

    // Test change user role
    @Test
    public void changeRoleWithoutAuthShouldReturn401() throws Exception {
        testChangeUserRole(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void changeRoleGuestShouldReturn403() throws Exception {
        testChangeUserRole(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void changeRoleUserShouldReturn403() throws Exception {
        testChangeUserRole(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void changeRoleAdminShouldReturn200() throws Exception {
        testChangeUserRole(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void changeRoleSuperAdminShouldReturn200() throws Exception {
        testChangeUserRole(status().isOk());
    }

    private void testChangeUserRole(ResultMatcher expectedResult) throws Exception {
        JSONObject changeRolesRequest = new JSONObject();
        JSONArray roles = new JSONArray();
        roles.put("ROLE_USER");
        changeRolesRequest.put("roles", roles);

        when(userService.changeUserRoles(any(), any(), any())).thenReturn(true);
        mockMvc.perform(post("/api/users/update/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeRolesRequest.toString()))
                .andExpect(expectedResult);
    }

    // Test get user
    @Test
    public void getUserWithoutAuthShouldReturn401() throws Exception {
        testGetUser(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void getUserGuestShouldReturn403() throws Exception {
        testGetUser(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getUserUserShouldReturn200() throws Exception {
        testGetUser(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getUserAdminShouldReturn200() throws Exception {
        testGetUser(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void getUserSuperAdminShouldReturn200() throws Exception {
        testGetUser(status().isOk());
    }

    private void testGetUser(ResultMatcher expectedResult) throws Exception {
        UserDTO mockResult = new UserDTO();
        when(userService.findById(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/users/"))
                .andExpect(expectedResult);
    }

    // Test get all users
    @Test
    public void getAllUsersWithoutAuthShouldReturn401() throws Exception {
        testGetAllUsers(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void getAllUsersGuestShouldReturn403() throws Exception {
        testGetAllUsers(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllUsersUserShouldReturn403() throws Exception {
        testGetAllUsers(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getAllUsersAdminShouldReturn200() throws Exception {
        testGetAllUsers(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void getAllUsersSuperAdminShouldReturn200() throws Exception {
        testGetAllUsers(status().isOk());
    }

    private void testGetAllUsers(ResultMatcher expectedResult) throws Exception {
        ListUserResponse userResponse = new ListUserResponse();
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "name"));
        PageImpl<ListUserResponse> page = new PageImpl<>(List.of(userResponse), pageRequest, 1);

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/all")).andExpect(expectedResult);
    }

    // Test delete user
    @Test
    public void deleteUserWithoutAuthShouldReturn401() throws Exception {
        deleteUserTest(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void deleteUserGuestShouldReturn403() throws Exception {
        deleteUserTest(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void deleteUserUserShouldReturn200() throws Exception {
        deleteUserTest(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteUserAdminShouldReturn200() throws Exception {
        deleteUserTest(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void deleteUserSuperAdminShouldReturn200() throws Exception {
        deleteUserTest(status().isOk());
    }

    private void deleteUserTest(ResultMatcher expectedResult) throws Exception {
        when(userService.deleteUser(any(), any(), any())).thenReturn(true);

        mockMvc.perform(delete("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(expectedResult);
    }

    @Test
    public void recoverWithoutAuthShouldReturn200() throws Exception {
        recoverTest(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void recoverGuestShouldReturn200() throws Exception {
        recoverTest(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void recoverUserShouldReturn200() throws Exception {
        recoverTest(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void recoverAdminShouldReturn200() throws Exception {
        recoverTest(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    public void recoverSuperAdminShouldReturn200() throws Exception {
        recoverTest(status().isOk());
    }

    private void recoverTest(ResultMatcher expectedResult) throws Exception {
        mockMvc.perform(post("/api/users/recover?email=test@test.com"))
                .andExpect(expectedResult);
    }
}
