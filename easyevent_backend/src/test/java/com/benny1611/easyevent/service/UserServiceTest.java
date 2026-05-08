package com.benny1611.easyevent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;


import com.benny1611.easyevent.auth.AuthenticatedUser;
import com.benny1611.easyevent.dao.*;
import com.benny1611.easyevent.dto.*;
import com.benny1611.easyevent.entity.*;
import com.benny1611.easyevent.exception.AccountSoftDeletedException;
import com.benny1611.easyevent.exception.RoleNotFoundException;
import com.benny1611.easyevent.util.JwtUtils;
import com.benny1611.easyevent.util.LocaleProvider;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProfileImageService profileImageService;
    @Mock private IMailService mailService;
    @Mock private EntityManager entityManager;
    @Mock private Session session;

    // These are required for the constructor but not used in the specific method
    @Mock private UserStateRepository userStateRepository;
    @Mock private UserBanLogRepository userBanLogRepository;
    @Mock private DeletionLogRepository logRepository;
    @Mock private UserRecoveryLogRepository recoveryLogRepository;
    @Mock private LocaleProvider localeProvider;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validRequest;
    private User targetUser;
    private ChangeUserRequest request;
    private AuthenticatedUser superAdminPrincipal;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "entityManager", entityManager);
        lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);

        validRequest = new CreateUserRequest();
        validRequest.setName("John Doe");
        validRequest.setEmail("john@example.com");
        validRequest.setPassword("securePassword");
        validRequest.setRoles(Set.of("ROLE_USER"));

        targetUser = new User();
        targetUser.setId(100L);
        targetUser.setName("Old Name");
        targetUser.setEmail("target@test.com");

        UserState state = new UserState();
        state.setName("ACTIVE");
        targetUser.setState(state);
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        targetUser.setRoles(Set.of(userRole));

        request = new ChangeUserRequest();
    }

    @Test
    @DisplayName("Should successfully create a user")
    void createUser_Success() throws IOException {
        // Arrange
        CreateUserRequest validRequest = new CreateUserRequest();
        validRequest.setEmail("test@test.com");
        validRequest.setRoles(Set.of("ROLE_USER"));

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        String picture = "smile";
        MockMultipartFile profilePicture = new MockMultipartFile(
                "profilePicture", "", "application/json", picture.getBytes()
        );
        MockMultipartFile emptyProfilePicture = new MockMultipartFile(
                "profilePicture", "", "application/json", new byte[]{}
        );
        when(profileImageService.saveAsPng(profilePicture, null)).thenReturn("/profile/picture");


        // Act
        User resultWithProfilePicture = userService.createUser(validRequest, profilePicture);
        User resultWithEmptyProfilePicture = userService.createUser(validRequest, emptyProfilePicture);
        User resultWithoutPicture = userService.createUser(validRequest, null);

        // Assert
        assertNotNull(resultWithProfilePicture);
        assertNotNull(resultWithEmptyProfilePicture);
        assertNotNull(resultWithoutPicture);
        verify(session, times(3)).disableFilter("deletedUserFilter"); // Verify it was called
        verify(userRepository, times(4)).save(any(User.class));
        verify(profileImageService, times(1)).saveAsPng(profilePicture, null);
    }

    @Test
    @DisplayName("Should throw exception when email is already taken (soft-deleted)")
    void createUser_ThrowsAccountSoftDeletedException() {
        // Arrange
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(AccountSoftDeletedException.class, () -> {
            userService.createUser(validRequest, null);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when attempting to create an Admin")
    void createUser_ThrowsAccessDeniedException_ForAdminRole() {
        // Arrange
        validRequest.setRoles(Set.of("ROLE_ADMIN"));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            userService.createUser(validRequest, null);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw RoleNotFoundException when role does not exist")
    void createUser_ThrowsRoleNotFoundException() {
        // Arrange
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoleNotFoundException.class, () -> {
            userService.createUser(validRequest, null);
        });
    }

    @Test
    @DisplayName("Should create user and download profile picture successfully")
    void createUser_WithUrl_Success() throws IOException {
        // Arrange
        String email = "oauth@test.com";
        String name = "OAuth User";
        String picUrl = "https://example.com/photo.jpg";
        byte[] mockBytes = new byte[]{1, 2, 3};

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        UserState activeState = new UserState();
        activeState.setName("ACTIVE");
        when(userStateRepository.findByName("ACTIVE")).thenReturn(Optional.of(activeState));

        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            if (u.getId() == null) u.setId((long) (Math.random() * 100000L)); // Simulate DB assigning ID
            return u;
        });

        when(profileImageService.saveAsPng(eq(mockBytes), any())).thenReturn("http://cdn.com/saved.png");

        // Mocking the static method
        try (MockedStatic<ProfileImageService> mockedStatic = mockStatic(ProfileImageService.class)) {
            mockedStatic.when(() -> ProfileImageService.downloadImage(picUrl)).thenReturn(mockBytes);

            // Act
            User result = userService.createUser(email, name, picUrl);

            // Assert
            assertNotNull(result);
            assertEquals("http://cdn.com/saved.png", result.getProfilePictureUrl());
            assertEquals(email, result.getEmail());
            verify(userRepository, times(2)).save(any(User.class));
        }
    }

    @Test
    @DisplayName("Should create user even if profile picture download fails")
    void createUser_WithUrl_DownloadFails() throws IOException {
        // Arrange
        String email = "oauth@test.com";
        String name = "OAuth User";
        String picUrl = "https://broken-link.com/photo.jpg";

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        UserState activeState = new UserState();
        activeState.setName("ACTIVE");
        when(userStateRepository.findByName("ACTIVE")).thenReturn(Optional.of(activeState));

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Simulate static method throwing an exception
        try (MockedStatic<ProfileImageService> mockedStatic = mockStatic(ProfileImageService.class)) {
            mockedStatic.when(() -> ProfileImageService.downloadImage(anyString()))
                    .thenThrow(new RuntimeException("Network Error"));

            // Act
            User result = userService.createUser(email, name, picUrl);

            // Assert
            assertNotNull(result);
            assertNull(result.getProfilePictureUrl(), "Profile URL should be null if download fails");
            // Should only save once because the second save is inside the if(profilePicUrl != null) block
            verify(userRepository, times(1)).save(any(User.class));
            verify(profileImageService, never()).saveAsPng((MultipartFile) any(), any());
        }
    }

    @Test
    @DisplayName("Should throw RuntimeException when ROLE_USER is missing in DB")
    void createUser_RoleNotFound_ThrowsException() {
        // Arrange
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            userService.createUser("test@test.com", "Test", null);
        });

        assertEquals("Could not find the user role", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return UserDTO when principal and user exist")
    void findById_Success() {
        // Arrange
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(principal.getUserId()).thenReturn(1L);

        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setLanguage("en");
        user.setProfilePictureUrl("http://image.url");
        user.setPassword(null); // Testing the logic: setLocalPasswordSet(true) if null

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserDTO result = userService.findById(principal);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertTrue(result.isLocalPasswordSet()); // Based on user.getPassword() == null
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return UserDTO when principal and user exist")
    void findById_Success_2() {
        // Arrange
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(principal.getUserId()).thenReturn(1L);

        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setLanguage("en");
        user.setProfilePictureUrl("http://image.url");
        user.setPassword("test"); // Testing the logic: setLocalPasswordSet(true) if null

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserDTO result = userService.findById(principal);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertFalse(result.isLocalPasswordSet()); // Based on user.getPassword() == null
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return null when principal or ID is null")
    void findById_NullInput() {
        assertNull(userService.findById(null));

        AuthenticatedUser principalNoId = mock(AuthenticatedUser.class);
        when(principalNoId.getUserId()).thenReturn(null);
        assertNull(userService.findById(principalNoId));
    }

    @Test
    @DisplayName("Should return null when user is not found in database")
    void findById_NotFound() {
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(principal.getUserId()).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(userService.findById(principal));
    }

    @Test
    @DisplayName("Should delegate call to repository")
    void findByEmail_Delegate() {
        String email = "find@me.com";
        userService.findByEmail(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should activate user and clear token when token is valid")
    void activateUser_Success() {
        // Arrange
        UUID token = UUID.randomUUID();
        User user = new User();
        user.setActivationToken(token);

        UserState activeState = new UserState();
        activeState.setName("ACTIVE");
        when(userStateRepository.findByName("ACTIVE")).thenReturn(Optional.of(activeState));

        when(userRepository.findByActivationToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.activateUser(token);

        // Assert
        assertNotNull(result);
        assertNull(result.getActivationToken());
        assertNull(result.getActivationSentAt());
        // Verify setUserStateActive was effectively called (assuming it changes a field)
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should return null if token does not exist")
    void activateUser_InvalidToken() {
        UUID token = UUID.randomUUID();
        when(userRepository.findByActivationToken(token)).thenReturn(Optional.empty());

        User result = userService.activateUser(token);

        assertNull(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update token and send mail when user is NOT active")
    void resendActivation_UserNotActive_Success() {
        // Arrange
        String email = "inactive@test.com";
        User user = new User();
        user.setEmail(email);

        // Mocking the state hierarchy: user -> state -> name
        UserState inactiveState = mock(UserState.class);
        when(inactiveState.getName()).thenReturn("PENDING");
        user.setState(inactiveState);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.resendActivation(email);

        // Assert
        assertNotNull(user.getActivationToken(), "A new token should have been generated");
        assertNotNull(user.getActivationSentAt(), "Timestamp should be set");

        verify(userRepository).save(user);
        verify(mailService).sendActivationEmail(user);
    }

    @Test
    @DisplayName("Should do nothing if user is already ACTIVE")
    void resendActivation_UserAlreadyActive_DoesNothing() {
        // Arrange
        String email = "active@test.com";
        User user = new User();

        UserState activeState = mock(UserState.class);
        when(activeState.getName()).thenReturn("ACTIVE");
        user.setState(activeState);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        userService.resendActivation(email);

        // Assert
        verify(userRepository, never()).save(any());
        verify(mailService, never()).sendActivationEmail(any());
    }

    @Test
    @DisplayName("Should do nothing if user is not found")
    void resendActivation_UserNotFound_DoesNothing() {
        // Arrange
        String email = "ghost@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        userService.resendActivation(email);

        // Assert
        verify(userRepository, never()).save(any());
        verify(mailService, never()).sendActivationEmail(any());
    }

    @Test
    @DisplayName("Should generate a unique token every time it is called")
    void resendActivation_GeneratesNewToken() {
        // Arrange
        String email = "inactive@test.com";
        User user = new User();
        UUID oldToken = UUID.randomUUID();
        user.setActivationToken(oldToken);

        UserState inactiveState = mock(UserState.class);
        when(inactiveState.getName()).thenReturn("INACTIVE");
        user.setState(inactiveState);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        userService.resendActivation(email);

        // Assert
        assertNotEquals(oldToken, user.getActivationToken(), "The old token should have been replaced");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Super Admin should be able to modify anyone")
    void updateByAdmin_SuperAdmin_Success() throws IOException {
        // Arrange
        AuthenticatedUser superAdmin = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .when(superAdmin).getAuthorities();

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        targetUser.setName("test");

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        request.setName("test");

        String picture = "smile";
        MockMultipartFile profilePicture = new MockMultipartFile(
                "profilePicture", "", "application/json", picture.getBytes()
        );
        MockMultipartFile emptyProfilePicture = new MockMultipartFile(
                "profilePicture", "", "application/json", new byte[]{}
        );

        when(profileImageService.saveAsPng(profilePicture, 100L)).thenReturn("/saved/pic");

        // Act
        ListUserResponse response = userService.updateUserByAdmin(superAdmin, 100L, request, null);
        targetUser.setName("test2");
        ListUserResponse response2 = userService.updateUserByAdmin(superAdmin, 100L, request, profilePicture);
        request.setName(null);
        ListUserResponse response3 = userService.updateUserByAdmin(superAdmin, 100L, request, emptyProfilePicture);


        // Assert
        assertNotNull(response);
        assertNotNull(response2);
        assertNotNull(response3);
        verify(userRepository, times(1)).save(targetUser);
    }

    @Test
    @DisplayName("Admin should be able to modify a standard USER")
    void updateByAdmin_AdminModifyingUser_Success() throws IOException {
        // Arrange
        AuthenticatedUser admin = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(admin).getAuthorities();

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        targetUser.setRoles(Set.of(userRole));
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        request.setName("test");

        // Act
        ListUserResponse response = userService.updateUserByAdmin(admin, 100L, request, null);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("Admin should NOT be able to modify another ADMIN")
    void updateByAdmin_AdminModifyingAdmin_ReturnsNull() throws IOException {
        // Arrange
        AuthenticatedUser admin = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(admin).getAuthorities();
        when(admin.getUserId()).thenReturn(50L); // Different ID

        Role userRole = new Role();
        userRole.setName("ROLE_ADMIN");
        targetUser.setRoles(Set.of(userRole));
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        // Act
        ListUserResponse response = userService.updateUserByAdmin(admin, 100L, request, null);

        // Assert
        assertNull(response, "Admin should not have permission to modify another Admin");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("User should be able to modify themselves (ID match)")
    void updateByAdmin_SelfUpdate_Success() throws IOException {
        // Arrange
        AuthenticatedUser userPrincipal = mock(AuthenticatedUser.class);
        when(userPrincipal.getUserId()).thenReturn(100L); // IDs match
        // No special roles
        doReturn(Collections.emptyList()).when(userPrincipal).getAuthorities();

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        request.setName("test");

        // Act
        ListUserResponse response = userService.updateUserByAdmin(userPrincipal, 100L, request, null);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("User should NOT be able to modify someone else")
    void updateByAdmin_OtherUser_ReturnsNull() throws IOException {
        // Arrange
        AuthenticatedUser otherUser = mock(AuthenticatedUser.class);
        when(otherUser.getUserId()).thenReturn(999L); // ID mismatch
        doReturn(Collections.emptyList()).when(otherUser).getAuthorities();

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        targetUser.setRoles(Set.of(userRole));
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        // Act
        ListUserResponse response = userService.updateUserByAdmin(otherUser, 100L, request, null);

        // Assert
        assertNull(response);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update email and name when valid")
    void updateBySuperAdmin_Success() throws IOException {
        // Arrange
        request.setName("New Name");
        request.setEmail("new@test.com");
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        superAdminPrincipal = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .when(superAdminPrincipal).getAuthorities();

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        // changeMailAddress checks if the new email is already taken
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        // Act
        ListUserResponse response = userService.updateUserBySuperAdmin(superAdminPrincipal, 100L, request, mockFile);

        // Assert
        assertNotNull(response);
        assertEquals("New Name", targetUser.getName());
        assertEquals("new@test.com", targetUser.getEmail());
        verify(userRepository).save(targetUser); // Verify save was triggered
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if new email is already in use")
    void updateBySuperAdmin_EmailConflict_ThrowsException() {
        // Arrange
        request.setEmail("taken@test.com");

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        // Simulate another user already having this email
        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));

        superAdminPrincipal = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .when(superAdminPrincipal).getAuthorities();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUserBySuperAdmin(superAdminPrincipal, 100L, request, null);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update profile picture and trigger save")
    void updateBySuperAdmin_ProfilePic_Success() throws IOException {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        when(profileImageService.saveAsPng(eq(mockFile), eq(100L))).thenReturn("/new-pic.png");

        superAdminPrincipal = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .when(superAdminPrincipal).getAuthorities();

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        // Act
        ListUserResponse response = userService.updateUserBySuperAdmin(superAdminPrincipal, 100L, request, mockFile);

        // Assert
        assertNotNull(response);
        assertEquals("/new-pic.png", targetUser.getProfilePictureUrl());
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("Should NOT call save if no data actually changed")
    void updateBySuperAdmin_NoChanges_DoesNotSave() throws IOException {
        // Arrange
        request.setName("Old Name"); // Same as target
        request.setEmail("target@test.com"); // Same as target

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        superAdminPrincipal = mock(AuthenticatedUser.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .when(superAdminPrincipal).getAuthorities();

        // Act
        userService.updateUserBySuperAdmin(superAdminPrincipal, 100L, request, null);

        // Assert
        verify(userRepository, never()).save(any());
        // findByEmail should never be called because the email didn't change from target@test.com
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should return null if non-admin tries to call this method")
    void updateBySuperAdmin_NotAuthorized_ReturnsNull() throws IOException {
        // Arrange
        AuthenticatedUser plebUser = mock(AuthenticatedUser.class);
        when(plebUser.getUserId()).thenReturn(99L);
        when(plebUser.getAuthorities()).thenReturn(List.of()); // No roles

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        // Act
        ListUserResponse response = userService.updateUserBySuperAdmin(plebUser, 100L, request, null);

        // Assert
        assertNull(response);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_SuccessfulBasicInfoUpdate() throws IOException {
        // Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setName("New Name");
        userDTO.setEmail("new@example.com");
        userDTO.setOldPassword("oldPassword");
        userDTO.setNewPassword("newPassword");
        targetUser.setPassword("oldPasswordHash");

        AuthenticatedUser mockPrincipal = mock(AuthenticatedUser.class);
        when(mockPrincipal.getUserId()).thenReturn(100L);

        UserState inactiveState = new UserState();
        inactiveState.setName("INACTIVE");
        when(userStateRepository.findByName("INACTIVE")).thenReturn(Optional.of(inactiveState));
        when(passwordEncoder.matches("oldPassword", "oldPasswordHash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newPassword");
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        when(jwtUtils.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        // Act
        UserDTO result = userService.updateUser(mockPrincipal, userDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("mock-jwt-token", result.getToken());
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUser_ChangePassword_Success() throws IOException {
        // Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setOldPassword("correctPassword");
        userDTO.setNewPassword("newSecret");

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.encode("newSecret")).thenReturn("encodedNewSecret");

        AuthenticatedUser mockPrincipal = mock(AuthenticatedUser.class);
        when(mockPrincipal.getUserId()).thenReturn(100L);

        // Act
        UserDTO result = userService.updateUser(mockPrincipal, userDTO, null);

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode("newSecret");
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUser_ChangePassword_WrongOldPassword_ThrowsException() {
        // Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setOldPassword("wrongPassword");
        userDTO.setNewPassword("newSecret");
        targetUser.setPassword("encodedPassword");

        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        AuthenticatedUser mockPrincipal = mock(AuthenticatedUser.class);
        when(mockPrincipal.getUserId()).thenReturn(100L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(mockPrincipal, userDTO, null);
        });
    }

    @Test
    void updateUser_WithProfilePicture() throws IOException {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        when(profileImageService.saveAsPng((MultipartFile) any(), anyLong())).thenReturn("http://images.com/profile.png");

        AuthenticatedUser mockPrincipal = mock(AuthenticatedUser.class);
        when(mockPrincipal.getUserId()).thenReturn(100L);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setName("Old Name");
        userDTO.setEmail("target@test.com");

        // Act
        UserDTO result = userService.updateUser(mockPrincipal, userDTO, mockFile);

        // Assert
        assertEquals("http://images.com/profile.png", result.getProfilePicture());
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUser_LanguageUpdate() throws IOException {
        // Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setLanguage("fr");
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));
        when(localeProvider.supports(any(Locale.class))).thenReturn(true);

        AuthenticatedUser mockPrincipal = mock(AuthenticatedUser.class);
        when(mockPrincipal.getUserId()).thenReturn(100L);

        // Act
        UserDTO result = userService.updateUser(mockPrincipal, userDTO, null);

        // Assert
        assertEquals("fr", result.getLanguage());
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUser_NoChanges_ReturnsNull() throws IOException {
        // Arrange
        // DTO has same values as existingUser, or no values set
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        when(userRepository.findByIdWithRolesAndState(100L)).thenReturn(Optional.of(targetUser));

        AuthenticatedUser mockPrincipal = mock(AuthenticatedUser.class);
        when(mockPrincipal.getUserId()).thenReturn(100L);

        // Act
        UserDTO result = userService.updateUser(mockPrincipal, userDTO, null);

        // Assert
        assertNull(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAllUsers_ShouldReturnMappedPage() {
        // --- 1. Arrange ---
        Pageable pageable = PageRequest.of(0, 10);

        // Mocking the EntityManager/Session unwrap
        when(entityManager.unwrap(Session.class)).thenReturn(session);

        UserState bannedState = new UserState();
        bannedState.setName("BANNED");
        bannedState.setId((short)3);
        when(userStateRepository.findByName("BANNED")).thenReturn(Optional.of(bannedState));

        // Mocking the ID fetch
        List<Long> userIds = Arrays.asList(1L, 2L);
        Page<Long> idPage = new PageImpl<>(userIds, pageable, 2);
        when(userRepository.findUserIds(pageable)).thenReturn(idPage);

        // Mocking the User entity retrieval
        User user1 = createMockUser(1L, "Alice", "ACTIVE");
        User user2 = createMockUser(2L, "Bob", "BANNED");
        when(userRepository.findAllByIdWithRolesAndState(userIds)).thenReturn(Arrays.asList(user1, user2));
        // Wait, the repository returns actual User objects:
        when(userRepository.findAllByIdWithRolesAndState(userIds)).thenReturn(Arrays.asList(user1, user2));

        // --- 2. Act ---
        Page<ListUserResponse> result = userService.getAllUsers(pageable);

        // --- 3. Assert ---
        assertNotNull(result);
        assertEquals(2, result.getContent().size());

        // Verify Filter behavior
        verify(session).disableFilter("deletedUserFilter");

        // Verify Mapping
        ListUserResponse firstUser = result.getContent().getFirst();
        assertEquals("Alice", firstUser.getName());
        assertTrue(firstUser.isActive());

        ListUserResponse secondUser = result.getContent().get(1);
        assertEquals("Bob", secondUser.getName());
        assertFalse(secondUser.isActive());

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getAllUsers_WhenEmpty_ShouldReturnEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(userRepository.findUserIds(pageable)).thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        Page<ListUserResponse> result = userService.getAllUsers(pageable);

        // Assert
        assertTrue(result.getContent().isEmpty());
        verify(userRepository, times(1)).findAllByIdWithRolesAndState(any());
    }

    // Helper to build a User entity with nested objects
    private User createMockUser(Long id, String name, String stateName) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");

        UserState state = new UserState();
        state.setName(stateName);
        state.setId((short) 1);
        user.setState(state);

        Role role = new Role();
        role.setName("ROLE_USER");
        user.setRoles(Collections.singleton(role));

        return user;
    }

    private AuthenticatedUser createPrincipal(Long id, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new AuthenticatedUser(id, "actor@test.com", (List) authorities);
    }

    private User createUserEntity(Long id, String roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail("target@test.com");

        Role role = new Role();
        role.setName(roleName);
        user.setRoles(new HashSet<>(Collections.singleton(role)));
        return user;
    }

    // --- Ban Tests ---

    @Test
    void banUserById_SuperAdminBanningAdmin_Success() {
        // Arrange
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_SUPER_ADMIN");
        User target = createUserEntity(2L, "ROLE_ADMIN");
        User actor = createUserEntity(1L, "ROLE_SUPER_ADMIN");

        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(actor));

        // Act
        boolean result = userService.banUserById(principal, 2L, new BanRequest("Rule break"));

        // Assert
        assertTrue(result, "Super Admin should bypass role checks");
        verify(userBanLogRepository).save(any());
    }

    @Test
    void banUserById_AdminBanningUser_Success() {
        // Arrange
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_ADMIN");
        User target = createUserEntity(2L, "ROLE_USER"); // Admin can modify User
        User actor = createUserEntity(1L, "ROLE_ADMIN");

        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(actor));

        // Act
        boolean result = userService.banUserById(principal, 2L, new BanRequest("Spam"));

        // Assert
        assertTrue(result);
        verify(mailService).sendBanMail(eq(target), anyString());
    }

    @Test
    void banUserById_AdminBanningAnotherAdmin_ReturnsFalse() {
        // Arrange
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_ADMIN");
        User target = createUserEntity(2L, "ROLE_ADMIN"); // Admin cannot modify Admin

        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));

        // Act
        boolean result = userService.banUserById(principal, 2L, new BanRequest("Power struggle"));

        // Assert
        assertFalse(result, "Admin should not be able to ban another Admin");
        verify(userBanLogRepository, never()).save(any());
    }

    @Test
    void banUserById_SelfBan_ReturnsFalse() {
        // Arrange
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_SUPER_ADMIN");
        User target = createUserEntity(1L, "ROLE_SUPER_ADMIN");

        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(target));

        // Act
        boolean result = userService.banUserById(principal, 1L, new BanRequest("Self-harm"));

        // Assert
        assertFalse(result, "Logic explicitly prevents self-banning regardless of roles");
    }

    // --- Unban Tests ---

    @Test
    void unbanUserById_AdminUnbanningUser_Success() {
        // Arrange
        Long adminId = 1L;
        Long targetId = 2L;
        AuthenticatedUser principal = createPrincipal(adminId, "ROLE_ADMIN");
        User actor = createUserEntity(adminId, "ROLE_ADMIN");
        User target = createUserEntity(targetId, "ROLE_USER");

        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(adminId)).thenReturn(Optional.of(actor));

        // Act
        boolean result = userService.unbanUserById(principal, targetId);

        // Assert
        assertTrue(result, "Admin should be able to unban a different user");
        verify(userBanLogRepository).save(any(UserBanLog.class));
        verify(mailService).sendUnbanMail(target);
    }

    @Test
    void unbanUserById_SuperAdminUnbanningAdmin_Success() {
        // Arrange
        Long superId = 1L;
        Long targetId = 2L;
        AuthenticatedUser principal = createPrincipal(superId, "ROLE_SUPER_ADMIN");
        User actor = createUserEntity(superId, "ROLE_SUPER_ADMIN");
        User target = createUserEntity(targetId, "ROLE_ADMIN");

        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByIdWithRoles(superId)).thenReturn(Optional.of(actor));

        // Act
        boolean result = userService.unbanUserById(principal, targetId);

        // Assert
        assertTrue(result, "Super Admin should be able to unban an Admin");
    }

    @Test
    void unbanUserById_AdminAttemptingSelfUnban_ReturnsFalse() {
        // Arrange
        Long adminId = 1L;
        // Principal ID matches Target ID
        AuthenticatedUser principal = createPrincipal(adminId, "ROLE_ADMIN");
        User target = createUserEntity(adminId, "ROLE_ADMIN");

        when(userRepository.findByIdWithRoles(adminId)).thenReturn(Optional.of(target));

        // Act
        boolean result = userService.unbanUserById(principal, adminId);

        // Assert
        assertFalse(result, "Admins should not be able to unban themselves");
        verify(userBanLogRepository, never()).save(any());
    }

    @Test
    void unbanUserById_RegularUserAttemptingUnban_ReturnsFalse() {
        // Arrange
        Long userId = 1L;
        Long targetId = 2L;
        // Principal is just a ROLE_USER
        AuthenticatedUser principal = createPrincipal(userId, "ROLE_USER");
        User target = createUserEntity(targetId, "ROLE_USER");

        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));

        // Act
        boolean result = userService.unbanUserById(principal, targetId);

        // Assert
        assertFalse(result, "Regular users have no unban privileges in the new logic");
        verify(userBanLogRepository, never()).save(any());
    }

    @Test
    void unbanUserById_ActorNotFoundInDb_ThrowsException() {
        // Arrange
        Long adminId = 1L;
        Long targetId = 2L;
        AuthenticatedUser principal = createPrincipal(adminId, "ROLE_ADMIN");
        User target = createUserEntity(targetId, "ROLE_USER");

        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        // Target is found, but the admin's own record is missing
        when(userRepository.findByIdWithRoles(adminId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.unbanUserById(principal, targetId);
        }, "Should throw 'User not found' if actor is missing from DB");
    }

    @Test
    void changeUserRoles_ValidTransition_CallsChangeRoleSideEffects() {
        // Arrange
        Long adminId = 1L;
        Long userId = 2L;
        AuthenticatedUser principal = createPrincipal(adminId, "ROLE_ADMIN");

        // Target is a USER, we want to make them an ADMIN
        User target = createUserEntity(userId, "ROLE_USER");

        Role adminRoleEntity = new Role();
        adminRoleEntity.setName("ROLE_ADMIN");

        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(target));
        // We need this because changeRole(target, role) looks it up again
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRoleEntity));

        // Act
        boolean result = userService.changeUserRoles(principal, userId, new ChangeRolesRequest(List.of("ROLE_ADMIN")));

        // Assert
        assertTrue(result);

        // Verify changeRole logic:
        // 1. Target roles were cleared and updated
        assertEquals(1, target.getRoles().size());
        assertTrue(target.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN")));

        // 2. Repository save was called
        verify(userRepository).save(target);

        // 3. Email was sent
        verify(mailService).sendRoleChangeMail(eq(target), eq("ROLE_USER"), eq("ROLE_ADMIN"));
    }

    @Test
    void changeUserRoles_ForbiddenTransition_DoesNotCallChangeRole() {
        // Arrange
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_ADMIN");
        User target = createUserEntity(2L, "ROLE_ADMIN"); // Target is already ADMIN

        // Admin trying to promote another Admin to SuperAdmin (Forbidden)
        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));

        // Act
        boolean result = userService.changeUserRoles(principal, 2L, new ChangeRolesRequest(List.of("ROLE_SUPER_ADMIN")));

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any());
        verify(mailService, never()).sendRoleChangeMail(any(), any(), any());
    }

    @Test
    void changeUserRoles_NoOp_ReturnsTrueWithoutSaving() {
        // Arrange
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_ADMIN");
        User target = createUserEntity(2L, "ROLE_USER");

        // Target already is ROLE_USER, and request is ROLE_USER
        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));

        // Act
        boolean result = userService.changeUserRoles(principal, 2L, new ChangeRolesRequest(List.of("ROLE_USER")));

        // Assert
        assertTrue(result);
        // Should NOT call save or email because no change happened
        verify(userRepository, never()).save(any());
        verify(mailService, never()).sendRoleChangeMail(any(), any(), any());
    }

    @Test
    void changeUserRoles_SuperAdminDemotingAdminToUser_Success() {
        // --- 1. Arrange ---
        Long superAdminId = 1L;
        Long targetAdminId = 2L;

        // Actor is a Super Admin
        AuthenticatedUser principal = createPrincipal(superAdminId, "ROLE_SUPER_ADMIN");

        // Target is currently an Admin
        User targetUser = createUserEntity(targetAdminId, "ROLE_ADMIN");

        // Role objects for the transition
        Role userRoleEntity = new Role();
        userRoleEntity.setName("ROLE_USER");

        // Mocking behavior
        when(userRepository.findByIdWithRoles(targetAdminId)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRoleEntity));

        // --- 2. Act ---
        boolean result = userService.changeUserRoles(
                principal,
                targetAdminId,
                new ChangeRolesRequest(List.of("ROLE_USER"))
        );

        // --- 3. Assert ---
        assertTrue(result, "Super Admin should be allowed to demote an Admin to User");

        // Verify target now has the USER role
        assertTrue(targetUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_USER")));

        // Verify side effects
        verify(userRepository).save(targetUser);
        verify(mailService).sendRoleChangeMail(targetUser, "ROLE_ADMIN", "ROLE_USER");
    }


    @Test
    void deleteUser_SelfDeletion_Success() {
        // --- Arrange ---
        Long userId = 10L;
        AuthenticatedUser principal = createPrincipal(userId, "ROLE_USER");
        User target = createUserEntity(userId, "ROLE_USER");
        DeletionReason reasonObj = new DeletionReason("I want to leave");

        mockInactiveState();

        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(target));

        // --- Act ---
        boolean result = userService.deleteUser(principal, userId, reasonObj);

        // --- Assert ---
        assertTrue(result);
        assertNotNull(target.getDeletedAt());

        // Verify Log entry
        ArgumentCaptor<UserDeletionLog> logCaptor = ArgumentCaptor.forClass(UserDeletionLog.class);
        verify(logRepository).save(logCaptor.capture());

        UserDeletionLog savedLog = logCaptor.getValue();
        assertEquals("SELF", savedLog.getDeletionType());
        assertEquals(userId, savedLog.getTargetUserId());
        assertEquals("self-deleted", savedLog.getReason());

        verify(mailService).sendDeletionMail(eq(target), eq(false), eq("self-deleted"));
    }

    @Test
    void deleteUser_SuperAdminDeletingUser_Success() {
        // --- Arrange ---
        Long adminId = 1L;
        Long targetId = 2L;
        AuthenticatedUser principal = createPrincipal(adminId, "ROLE_SUPER_ADMIN");
        User target = createUserEntity(targetId, "ROLE_USER");
        DeletionReason reasonObj = new DeletionReason("Violated terms");

        mockInactiveState();

        when(userRepository.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));

        // --- Act ---
        boolean result = userService.deleteUser(principal, targetId, reasonObj);

        // --- Assert ---
        assertTrue(result);

        ArgumentCaptor<UserDeletionLog> logCaptor = ArgumentCaptor.forClass(UserDeletionLog.class);
        verify(logRepository).save(logCaptor.capture());

        UserDeletionLog savedLog = logCaptor.getValue();
        assertEquals("ADMIN", savedLog.getDeletionType());
        assertEquals("Violated terms", savedLog.getReason());

        verify(mailService).sendDeletionMail(eq(target), eq(true), eq("Violated terms"));
    }

    @Test
    void deleteUser_SuperAdminDeletingAnotherSuperAdmin_ReturnsFalse() {
        // --- Arrange ---
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_SUPER_ADMIN");
        User target = createUserEntity(2L, "ROLE_SUPER_ADMIN");

        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));

        // --- Act ---
        boolean result = userService.deleteUser(principal, 2L, new DeletionReason("Reason"));

        // --- Assert ---
        assertFalse(result, "Super Admins should not be able to delete other Super Admins");
        verify(logRepository, never()).save(any());
    }

    @Test
    void deleteUser_SuperAdminMissingReason_ThrowsException() {
        // --- Arrange ---
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_SUPER_ADMIN");
        User target = createUserEntity(2L, "ROLE_USER");
        DeletionReason emptyReason = new DeletionReason("");

        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));

        // --- Act & Assert ---
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.deleteUser(principal, 2L, emptyReason)
        );
        assertEquals("Reason cannot be null or empty", ex.getMessage());
    }

    @Test
    void deleteUser_RegularAdminTryingToDeleteUser_ReturnsFalse() {
        // --- Arrange ---
        // Principal is an ADMIN, but NOT a SUPER_ADMIN
        AuthenticatedUser principal = createPrincipal(1L, "ROLE_ADMIN");
        User target = createUserEntity(2L, "ROLE_USER");

        when(userRepository.findByIdWithRoles(2L)).thenReturn(Optional.of(target));

        // --- Act ---
        boolean result = userService.deleteUser(principal, 2L, new DeletionReason("Reason"));

        // --- Assert ---
        assertFalse(result, "Only Super Admins (or the user themselves) can delete accounts");
        verify(logRepository, never()).save(any());
    }

    private void mockInactiveState() {
        UserState inactiveState = new UserState();
        inactiveState.setName("INACTIVE");
        when(userStateRepository.findByName("INACTIVE")).thenReturn(Optional.of(inactiveState));
    }
}
