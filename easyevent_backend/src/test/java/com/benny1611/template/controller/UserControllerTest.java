package com.benny1611.template.controller;

import com.benny1611.template.auth.AuthenticatedUser;
import com.benny1611.template.dao.RoleRepository;
import com.benny1611.template.dto.CreateUserRequest;
import com.benny1611.template.dto.ListUserResponse;
import com.benny1611.template.dto.UserDTO;
import com.benny1611.template.entity.User;
import com.benny1611.template.service.UserService;
import com.benny1611.template.util.JwtUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RoleRepository roleRepository;

    @TestConfiguration
    static class TestSecurityConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.getParameterType().equals(AuthenticatedUser.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter,
                                              ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest,
                                              WebDataBinderFactory binderFactory) {

                    return webRequest.getAttribute("TEST_USER", NativeWebRequest.SCOPE_REQUEST);
                }
            });
        }
    }

    private final SimpleGrantedAuthority userAuthority = new SimpleGrantedAuthority("ROLE_USER");
    private final AuthenticatedUser user = new AuthenticatedUser(42L, "test@test.com", List.of(userAuthority));

    private final SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
    private final AuthenticatedUser admin = new AuthenticatedUser(43L, "test@test.com", List.of(adminAuthority));

    @Test
    void userCreateSuccessTest() throws Exception {
        CreateUserRequest mockRequest = new CreateUserRequest();
        mockRequest.setEmail("test@email.com");
        mockRequest.setName("test");
        mockRequest.setPassword("Password1234!!");
        mockRequest.setRoles(Set.of("USER_ROLE"));

        User user = new User();

        when(userService.createUser(mockRequest, null)).thenReturn(user);

        mockMvc.perform(multipart("/api/users/create")
                        .param("email", "test@email.com")
                        .param("password", "Password1234!!")
                        .param("name", "test")
                        .param("roles", "USER_ROLE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    void incorrectDataTest() throws Exception {
        // missing mail
        mockMvc.perform(multipart("/api/users/create")
                        .param("password", "Password1234!!")
                        .param("name", "test")
                        .param("roles", "USER_ROLE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        // incorrect mail
        mockMvc.perform(multipart("/api/users/create")
                        .param("email", "testemail.com")
                        .param("password", "Password1234!!")
                        .param("name", "test")
                        .param("roles", "USER_ROLE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        // missing password
        mockMvc.perform(multipart("/api/users/create")
                        .param("email", "test@email.com")
                        .param("name", "test")
                        .param("roles", "USER_ROLE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        // password not strong enough
        mockMvc.perform(multipart("/api/users/create")
                        .param("email", "test@email.com")
                        .param("password", "test")
                        .param("name", "test")
                        .param("roles", "USER_ROLE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        // missing name
        mockMvc.perform(multipart("/api/users/create")
                        .param("email", "test@email.com")
                        .param("password", "Password1234!!")
                        .param("roles", "USER_ROLE")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        // missing roles
        mockMvc.perform(multipart("/api/users/create")
                        .param("email", "test@email.com")
                        .param("password", "Password1234!!")
                        .param("name", "test")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void activationTest() throws Exception {
        UUID token = UUID.randomUUID();
        User user = new User();

        when(userService.activateUser(token)).thenReturn(user);
        JSONObject jo = new JSONObject();
        jo.put("token", token);

        mockMvc.perform(post("/api/users/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(jo.toString()))
                .andExpect(status().isOk());

        UUID fakeToken = UUID.randomUUID();
        JSONObject fakeTokenJo = new JSONObject();
        fakeTokenJo.put("token", fakeToken);

        when(userService.activateUser(fakeToken)).thenReturn(null);
        mockMvc.perform(post("/api/users/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(fakeTokenJo.toString()))
                .andExpect(status().isNotFound());

        JSONObject emptyJSON = new JSONObject();
        mockMvc.perform(post("/api/users/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJSON.toString()))
                .andExpect(status().isBadRequest());

        String invalidJSONString = "test";
        mockMvc.perform(post("/api/users/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJSONString))
                .andExpect(status().isBadRequest());


        JSONObject nullTokenJo = new JSONObject();
        nullTokenJo.put("token", null);

        mockMvc.perform(post("/api/users/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(nullTokenJo.toString()))
                .andExpect(status().isBadRequest());

        JSONObject blankTokenJo = new JSONObject();
        blankTokenJo.put("token", "");

        mockMvc.perform(post("/api/users/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(blankTokenJo.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void resendActivationTest() throws Exception {
        mockMvc.perform(post("/api/users/resend-activation"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users/resend-activation?email=test"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users/resend-activation?email=test@email.com"))
                .andExpect(status().isOk());
    }

    @Test
    public void updateUserTest() throws Exception {
        String userDtoJson = """
    {
        "name": "test",
        "id": 1,
        "email": "test@test.com",
        "language": "en",
        "oldPassword": "test",
        "newPassword": "test1234!"
    }
    """;
        String userDtoJson2 = """
    {
        "name": "test",
        "id": 2,
        "email": "test@test.com",
        "language": "en",
        "oldPassword": "test",
        "newPassword": "test1234!"
    }
    """;

        MockMultipartFile userPart = new MockMultipartFile(
                "userDTO", "", "application/json", userDtoJson.getBytes()
        );

        MockMultipartFile userPart2 = new MockMultipartFile(
                "userDTO", "", "application/json", userDtoJson2.getBytes()
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "profilePicture", "test.jpg", "image/jpeg", "data".getBytes()
        );

        UserDTO mockResponse = new UserDTO();
        mockResponse.setId(42L);

        when(userService.updateUser(any(), argThat(usr -> usr != null && usr.getId().equals(1L)), any()))
                .thenReturn(mockResponse);
        when(userService.updateUser(any(), argThat(usr -> usr != null && usr.getId().equals(2L)), any()))
                .thenReturn(null);
        when(userService.updateUser(isNull(), any(), any())).thenReturn(null);

        // All good test
        mockMvc.perform(multipart("/api/users/update")
                        .file(userPart)
                        .file(filePart)
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isOk());

        // All good test - no image
        mockMvc.perform(multipart("/api/users/update")
                        .file(userPart)
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isOk());

        // Empty request
        mockMvc.perform(multipart("/api/users/update")
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/users/update")
                        .file(userPart2)
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateUserByAdminTest() throws Exception {
        ListUserResponse mockResponse = new ListUserResponse();
        when(userService.updateUserByAdmin(any(), eq(1L), any(), any())).thenReturn(mockResponse);
        when(userService.updateUserByAdmin(any(), eq(2L), any(), any())).thenReturn(null);
        testUpdateUser( "/api/users/update/");
    }

    @Test
    public void updateUserBySuperAdminTest() throws Exception {
        ListUserResponse mockResponse = new ListUserResponse();
        when(userService.updateUserBySuperAdmin(any(), eq(1L), any(), any())).thenReturn(mockResponse);
        when(userService.updateUserBySuperAdmin(any(), eq(2L), any(), any())).thenReturn(null);
        testUpdateUser("/api/users/update/admin/");
    }

    private void testUpdateUser(String url) throws Exception {
        String userDtoJson = """
    {
        "name": "test",
        "email": "test@test.com"
    }
    """;

        MockMultipartFile userPart = new MockMultipartFile(
                "changeUserRequest", "", "application/json", userDtoJson.getBytes()
        );
        MockMultipartFile filePart = new MockMultipartFile(
                "profilePicture", "test.jpg", "image/jpeg", "data".getBytes()
        );

        // All good test
        mockMvc.perform(multipart(url + "1")
                        .file(userPart)
                        .file(filePart)
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isOk());

        // All good test - no image
        mockMvc.perform(multipart(url + "1")
                        .file(userPart)
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isOk());

        // service returns null test
        mockMvc.perform(multipart(url + "2")
                        .file(userPart)
                        .file(filePart)
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        // missing user part
        mockMvc.perform(multipart(url + "2")
                        .requestAttr("TEST_USER", user)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // Ban and unban user tests
    @Test
    public void testBanUser() throws Exception {

        JSONObject banRequest = new JSONObject();
        banRequest.put("reason", "test");

        when(userService.banUserById(any(), eq(1L), any())).thenReturn(true);
        when(userService.banUserById(any(), eq(2L), any())).thenReturn(false);

        // All ok test
        mockMvc.perform(post("/api/users/ban/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(banRequest.toString())
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isOk());

        // Missing content
        mockMvc.perform(post("/api/users/ban/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());

        // Bad input (missing reason)
        JSONObject badInput = new JSONObject();
        badInput.put("test", "test");
        mockMvc.perform(post("/api/users/ban/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badInput.toString())
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());

        // service returns false
        mockMvc.perform(post("/api/users/ban/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(banRequest.toString())
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUnbanUser() throws Exception {
        when(userService.unbanUserById(any(), eq(1L))).thenReturn(true);
        when(userService.unbanUserById(any(), eq(2L))).thenReturn(false);

        // All ok test
        mockMvc.perform(post("/api/users/unban/1")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isOk());

        // Not successful
        mockMvc.perform(post("/api/users/unban/2")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testChangeUserRole() throws Exception {

        JSONObject changeRolesRequest = new JSONObject();
        JSONArray roles = new JSONArray();
        roles.put("ROLE_USER");
        changeRolesRequest.put("roles", roles);

        when(userService.changeUserRoles(any(), eq(1L), any())).thenReturn(true);
        when(userService.changeUserRoles(any(), eq(2L), any())).thenReturn(false);

        // All ok test
        mockMvc.perform(post("/api/users/update/roles/1")
                        .requestAttr("TEST_USER", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeRolesRequest.toString()))
                .andExpect(status().isOk());

        // No content
        mockMvc.perform(post("/api/users/update/roles/1")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());

        // No id
        mockMvc.perform(post("/api/users/update/roles/")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().is4xxClientError());

        // Service returns false
        mockMvc.perform(post("/api/users/update/roles/2")
                        .requestAttr("TEST_USER", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeRolesRequest.toString()))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void testGetUser() throws Exception {
        UserDTO mockResult = new UserDTO();
        mockResult.setId(1L);
        when(userService.findById(argThat(usr -> usr != null && usr.getUserId().equals(42L)))).thenReturn(mockResult);
        when(userService.findById(argThat(usr -> usr != null && usr.getUserId().equals(43L)))).thenReturn(null);

        // All ok test
        mockMvc.perform(get("/api/users/")
                        .requestAttr("TEST_USER", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        // Service returns false
        mockMvc.perform(get("/api/users/")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void testGetAllUsers() throws Exception {
        ListUserResponse userResponse = new ListUserResponse();
        userResponse.setId(1L);
        userResponse.setName("test");
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "name"));
        PageImpl<ListUserResponse> page = new PageImpl<>(List.of(userResponse), pageRequest, 1);

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/all")
                        .requestAttr("TEST_USER", admin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.listUserResponseList[0].name").value("test"))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    public void deleteUserTest() throws Exception {

        when(userService.deleteUser(any(), eq(1L), any())).thenReturn(true);
        when(userService.deleteUser(any(), eq(2L), any())).thenReturn(false);

        // all ok test
        mockMvc.perform(delete("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("TEST_USER", user))
                .andExpect(status().isOk());

        // service returns false
        mockMvc.perform(delete("/api/users/2")
                        .requestAttr("TEST_USER", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // no id
        mockMvc.perform(delete("/api/users/")
                        .requestAttr("TEST_USER", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void recoverUserTest() throws Exception {

        mockMvc.perform(post("/api/users/recover?email=test@test.com")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/recover?email=")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/users/recover")
                        .requestAttr("TEST_USER", admin))
                .andExpect(status().isBadRequest());
    }
}
