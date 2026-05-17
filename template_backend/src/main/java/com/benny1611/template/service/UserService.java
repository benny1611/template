package com.benny1611.template.service;

import com.benny1611.template.auth.AuthenticatedUser;
import com.benny1611.template.dao.*;
import com.benny1611.template.dto.*;
import com.benny1611.template.entity.*;
import com.benny1611.template.exception.AccountSoftDeletedException;
import com.benny1611.template.exception.RoleNotFoundException;
import com.benny1611.template.util.JwtUtils;
import com.benny1611.template.util.LocaleProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class UserService {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private static final String ROLE_USER_STRING = "ROLE_USER";
    private static final String ROLE_ADMIN_STRING = "ROLE_ADMIN";
    private static final String ROLE_SUPER_ADMIN_STRING = "ROLE_SUPER_ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ProfileImageService profileImageService;
    private final UserStateRepository userStateRepository;
    private final UserBanLogRepository userBanLogRepository;
    private final DeletionLogRepository logRepository;
    private final UserRecoveryLogRepository recoveryLogRepository;
    private final IMailService mailService;
    private final LocaleProvider localeProvider;
    private final JwtUtils jwtUtils;

    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       ProfileImageService profileImageService,
                       @Qualifier("bcryptPasswordEncoder") PasswordEncoder passwordEncoder,
                       UserStateRepository userStateRepository,
                       UserBanLogRepository userBanLogRepository, DeletionLogRepository logRepository, UserRecoveryLogRepository recoveryLogRepository,
                       IMailService mailService,
                       LocaleProvider localeProvider, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.profileImageService = profileImageService;
        this.userStateRepository = userStateRepository;
        this.userBanLogRepository = userBanLogRepository;
        this.logRepository = logRepository;
        this.recoveryLogRepository = recoveryLogRepository;
        this.mailService = mailService;
        this.localeProvider = localeProvider;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public User createUser(CreateUserRequest createUserRequest, MultipartFile profilePicture) throws IOException {
        Session session = entityManager.unwrap(Session.class);

        // DISABLE the filter so we can see deleted users
        session.disableFilter("deletedUserFilter");

        Optional<User> softDeletedUserOptional = userRepository.findByEmail(createUserRequest.getEmail());
        if (softDeletedUserOptional.isPresent()) {
            throw new AccountSoftDeletedException(createUserRequest.getEmail());
        }

        User user = new User();

        user.setName(createUserRequest.getName());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));

        Set<String> roleNames = createUserRequest.getRoles();
        boolean isCreatingUser = !roleNames.contains(ROLE_ADMIN_STRING) && !roleNames.contains(ROLE_SUPER_ADMIN_STRING);
        if (isCreatingUser) {
            Set<Role> roles = roleNames.stream()
                    .map(roleName -> roleRepository.findByName(roleName).orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        } else {
            throw new AccessDeniedException("You can't perform that operation");
        }

        UUID activationToken = UUID.randomUUID();
        user.setActivationToken(activationToken);
        user.setActivationSentAt(Instant.now());

        user = userRepository.save(user);

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String profilePicUrl = profileImageService.saveAsPng(profilePicture, user.getId());

            user.setProfilePictureUrl(profilePicUrl);
            user = userRepository.save(user);
        }

        mailService.sendActivationEmail(user);

        return user;
    }

    @Transactional
    public User createUser(String email, String name, String pictureUrl) throws IOException {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(null);
        user.setActivationToken(null);
        user.setActivationSentAt(null);
        Role role = roleRepository.findByName(ROLE_USER_STRING).orElseThrow(() -> new RuntimeException("Could not find the user role"));
        user.setRoles(Set.of(role));
        setUserStateActive(user);

        user = userRepository.save(user);

        byte[] profilePicData;
        try {
            profilePicData = ProfileImageService.downloadImage(pictureUrl);
        } catch (IOException | InterruptedException | RuntimeException e) {
            profilePicData = null;
        }

        String profilePicUrl = null;
        if (profilePicData != null) {
            profilePicUrl = profileImageService.saveAsPng(profilePicData, user.getId());
        }
        if (profilePicUrl != null) {
            user.setProfilePictureUrl(profilePicUrl);
            user = userRepository.save(user);
        }

        return user;
    }

    public UserDTO findById(AuthenticatedUser principal) {
        UserDTO result = null;
        if (principal != null) {
            Long id = principal.getUserId();
            if (id != null) {
                Optional<User> userOptional = userRepository.findById(id);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    result = new UserDTO();
                    result.setEmail(user.getEmail());
                    result.setName(user.getName());
                    result.setLanguage(user.getLanguage());
                    result.setProfilePicture(user.getProfilePictureUrl());
                    result.setLocalPasswordSet(user.getPassword() == null);
                }
            }
        }
        return result;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User activateUser(UUID token) {
        Optional<User> userOptional = userRepository.findByActivationToken(token);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            setUserStateActive(user);
            user.setActivationToken(null);
            user.setActivationSentAt(null);
            userRepository.save(user);
            return user;
        } else {
            return null;
        }
    }

    public void resendActivation(@Email String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!user.getState().getName().equalsIgnoreCase("ACTIVE")) {
                UUID token = UUID.randomUUID();
                user.setActivationToken(token);
                user.setActivationSentAt(Instant.now());
                userRepository.save(user);
                mailService.sendActivationEmail(user);
            }
        }
    }

    public ListUserResponse updateUserByAdmin(AuthenticatedUser principal, Long userId, @Valid ChangeUserRequest changeUserRequest, MultipartFile profilePicture) throws IOException {
        User target = userRepository.findByIdWithRolesAndState(userId).orElseThrow(() -> new RuntimeException("Target user not found"));
        boolean canModify = canModifyUser(principal, target);
        ListUserResponse result = null;
        if (canModify) {
            result = new ListUserResponse();
            result.setId(target.getId());
            result.setActive(target.getState().getName().equalsIgnoreCase("ACTIVE"));
            result.setEmail(target.getEmail());
            result.setProfilePicture(target.getProfilePictureUrl());
            result.setBanned(isUserBanned(target));
            result.setDeletedAt(target.getDeletedAt());
            result.setSoftDeleted(target.isSoftDeleted());
            List<String> roles = target.getRoles().stream().map(Role::getName).toList();
            result.setRoles(roles);
            boolean used = false;
            if (changeUserRequest.getName() != null && !changeUserRequest.getName().equals(target.getName())) {
                String nameChange = changeUserRequest.getName();
                target.setName(nameChange);
                result.setName(nameChange);
                used = true;
            } else {
                result.setName(target.getName());
            }
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String profilePicUrl = profileImageService.saveAsPng(profilePicture, target.getId());
                target.setProfilePictureUrl(profilePicUrl);
                used = true;
            }
            if (used) {
                userRepository.save(target);
            }
        }
        return result;
    }

    public ListUserResponse updateUserBySuperAdmin(AuthenticatedUser principal, Long userId, @Valid ChangeUserRequest changeUserRequest, MultipartFile profilePicture) throws IOException {
        User target = userRepository.findByIdWithRolesAndState(userId).orElseThrow(() -> new RuntimeException("Target user not found"));
        boolean canModify = canModifyUser(principal, target);
        if (canModify) {
            ListUserResponse response = new ListUserResponse();
            boolean mailChanged = false;
            String newMailAddress = changeUserRequest.getEmail();
            if (newMailAddress != null) {
                if (!newMailAddress.isBlank()) { // just to be sure
                    mailChanged = changeMailAddress(newMailAddress, target, false);
                }
            }

            String newName = changeUserRequest.getName();
            boolean nameChanged = changeName(newName, target);

            boolean profilePictureChanged = false;
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String profilePicUrl = profileImageService.saveAsPng(profilePicture, target.getId());
                target.setProfilePictureUrl(profilePicUrl);
                profilePictureChanged = true;
            }

            if (nameChanged || mailChanged || profilePictureChanged) {
                userRepository.save(target);
            }
            response.setId(target.getId());
            response.setName(target.getName());
            response.setEmail(target.getEmail());
            response.setProfilePicture(target.getProfilePictureUrl());
            response.setActive(target.getState().getName().equalsIgnoreCase("ACTIVE"));
            response.setBanned(isUserBanned(target));
            List<String> roles = target.getRoles().stream().map(Role::getName).toList();
            response.setRoles(roles);
            response.setDeletedAt(target.getDeletedAt());
            response.setSoftDeleted(target.isSoftDeleted());
            return response;
        } else {
            return null;
        }
    }

    public UserDTO updateUser(AuthenticatedUser principal, UserDTO userDTO, MultipartFile profilePicture) throws IOException {
        UserDTO result = null;
        if (principal != null) {
            Long id = principal.getUserId();
            if (id != null) {
                Optional<User> userOptional = userRepository.findByIdWithRolesAndState(id);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    result = new UserDTO();
                    result.setLocalPasswordSet(user.getPassword() != null);
                    boolean used = false;
                    boolean refreshToken = false;

                    String newMailAddress = userDTO.getEmail();
                    boolean mailChanged = changeMailAddress(newMailAddress, user, true);
                    if (mailChanged) {
                        result.setEmail(newMailAddress);
                        used = true;
                        refreshToken = true;
                    }

                    String newName = userDTO.getName();
                    boolean nameChanged = changeName(newName, user);
                    if (nameChanged) {
                        result.setName(newName);
                        used = true;
                        refreshToken = true;
                    }

                    if (profilePicture != null && !profilePicture.isEmpty()) {
                        String profilePicUrl = profileImageService.saveAsPng(profilePicture, user.getId());
                        user.setProfilePictureUrl(profilePicUrl);
                        result.setProfilePicture(profilePicUrl);
                        used = true;
                        refreshToken = true;
                    }

                    if (userDTO.getLanguage() != null) {
                        String language = userDTO.getLanguage();
                        Locale requested = Locale.forLanguageTag(language);
                        if (localeProvider.supports(requested)) {
                            user.setLanguage(language);
                            result.setLanguage(language);
                            used = true;
                        }
                    }

                    if (userDTO.getNewPassword() != null) {
                        if (user.getPassword() == null) {
                            // OAuth-only user setting first password
                            user.setPassword(passwordEncoder.encode(userDTO.getNewPassword()));
                            refreshToken = true;
                            used = true;
                        } else {
                            // Password user changing password
                            if (!passwordEncoder.matches(userDTO.getOldPassword(), user.getPassword())) {
                                throw new IllegalArgumentException("PASSWORD_INCORRECT");
                            }
                            user.setPassword(passwordEncoder.encode(userDTO.getNewPassword()));
                            used = true;
                        }
                    }

                    if (used) {
                        userRepository.save(user);
                        if (refreshToken) {
                            String token = jwtUtils.generateToken(user);
                            result.setToken(token);
                        }
                    } else {
                        return null;
                    }
                }
            }
        }
        return result;
    }

    @Transactional
    public Page<ListUserResponse> getAllUsers(Pageable pageable) {
        Session session = entityManager.unwrap(Session.class);

        // DISABLE the filter so we can see deleted users
        session.disableFilter("deletedUserFilter");

        Page<Long> page = userRepository.findUserIds(pageable);
        List<User> users = userRepository.findAllByIdWithRolesAndState(page.getContent());

        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<ListUserResponse> dtos = page.getContent().stream()
                .map(userMap::get)
                .map(user -> {
                    ListUserResponse userDTO = new ListUserResponse();
                    userDTO.setId(user.getId());
                    userDTO.setName(user.getName());
                    userDTO.setEmail(user.getEmail());
                    userDTO.setProfilePicture(user.getProfilePictureUrl());
                    userDTO.setActive(user.getState().getName().equalsIgnoreCase("ACTIVE"));
                    userDTO.setBanned(isUserBanned(user));
                    userDTO.setDeletedAt(user.getDeletedAt());
                    userDTO.setSoftDeleted(user.isSoftDeleted());
                    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
                    userDTO.setRoles(roles);
                    return userDTO;
                }).toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private boolean changeMailAddress(String newMailAddress, User user, boolean sendActivationMail) {
        if (newMailAddress != null && !newMailAddress.equalsIgnoreCase(user.getEmail())) {
            Optional<User> checkIfEmailAlreadyExists = userRepository.findByEmail(newMailAddress);
            if (checkIfEmailAlreadyExists.isEmpty()) {
                user.setEmail(newMailAddress);

                if (sendActivationMail) {
                    setUserStateInactive(user);

                    UUID activationToken = UUID.randomUUID();
                    user.setActivationToken(activationToken);
                    user.setActivationSentAt(Instant.now());

                    mailService.sendActivationEmail(user);
                }
                return true;
            } else {
                throw new IllegalArgumentException("Email already in use");
            }
        } else {
            return false;
        }
    }

    private boolean changeName(String newName, User user) {
        if (newName != null && !newName.equals(user.getName())) {
            user.setName(newName);
            return true;
        } else {
            return false;
        }
    }

    private boolean canModifyUser(AuthenticatedUser principal, User target) {
        if (isSuperAdmin(principal)) {
            return true;
        }
        if (isAdmin(principal) && hasRole(target, ROLE_USER_STRING)) {
            return true;
        }
        return Objects.equals(principal.getUserId(), target.getId());
    }


    private boolean isUserBanned(User user) {
        UserState bannedState = userStateRepository.findByName("BANNED").orElseThrow(() -> new RuntimeException("Could not find BANNED state"));
        if (user.getState().getId() != null && bannedState.getId() != null) {
            return user.getState().getId().equals(bannedState.getId());
        } else {
            return false;
        }
    }

    public boolean banUserById(AuthenticatedUser principal, Long userId, BanRequest banRequest) {
        User target = userRepository.findByIdWithRoles(userId).orElseThrow(() -> new RuntimeException("Target user not found"));
        boolean canModify = canModifyUser(principal, target);
        if (Objects.equals(principal.getUserId(), target.getId())) {
            // A user can't ban themselves
            return false;
        }
        if (canModify) {
            User actor = userRepository.findByIdWithRoles(principal.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            UserBanLog userBanLog = new UserBanLog();
            userBanLog.setActionType(UserBanLog.ActionType.BAN);
            userBanLog.setAdmin(actor);
            userBanLog.setTargetUser(target);
            userBanLog.setReason(banRequest.getReason());
            userBanLogRepository.save(userBanLog);
            mailService.sendBanMail(target, banRequest.getReason());
            return true;
        } else {
            return false;
        }
    }

    public boolean unbanUserById(AuthenticatedUser principal, Long userId) {
        User target = userRepository.findByIdWithRoles(userId).orElseThrow(() -> new RuntimeException("Target user not found"));
        boolean canModify = (isSuperAdmin(principal) || isAdmin(principal)) && (principal.getUserId() != null && !principal.getUserId().equals(userId));
        if (canModify) {
            User actor = userRepository.findByIdWithRoles(principal.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            UserBanLog userBanLog = new UserBanLog();
            userBanLog.setActionType(UserBanLog.ActionType.UNBAN);
            userBanLog.setAdmin(actor);
            userBanLog.setTargetUser(target);
            userBanLog.setReason(null);
            userBanLogRepository.save(userBanLog);
            mailService.sendUnbanMail(target);
            return true;
        } else {
            return false;
        }
    }

    public boolean changeUserRoles(AuthenticatedUser principal, Long userId, ChangeRolesRequest changeRolesRequest) {
        List<String> requestedRoles = changeRolesRequest.getRoles();

        if (requestedRoles == null || requestedRoles.isEmpty()) {
            return true;
        }

        String targetRoleName = requestedRoles.getFirst();
        User target = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (hasRole(target, targetRoleName)) {
            return true;
        }

        if (!isAdmin(principal) && !isSuperAdmin(principal)) {
            return false;
        }


        if (isTransitionAllowed(principal, target, targetRoleName)) {
            changeRole(target, targetRoleName);
            return true;
        }

        return false;
    }

    private boolean isTransitionAllowed(AuthenticatedUser principal, User target, String newRole) {
        boolean isSelf = Objects.equals(principal.getUserId(), target.getId());

        return switch (newRole.toUpperCase()) {
            case ROLE_USER_STRING ->
                // Allow demotion to USER only if target is currently ADMIN
                // AND (Actor is SuperAdmin OR Actor is demoting themselves)
                    isAdmin(target) && (isSuperAdmin(principal) || isSelf);

            case ROLE_ADMIN_STRING ->
                // Allow promotion to ADMIN if target is currently USER
                // OR if the actor is an Admin/SuperAdmin modifying themselves
                    isUser(target) || isSelf;

            case ROLE_SUPER_ADMIN_STRING ->
                // Only SuperAdmins can grant SuperAdmin status
                    isSuperAdmin(principal);

            default -> false;
        };
    }

    private void changeRole(User target, String role) {
        Role userRole = roleRepository.findByName(role).orElseThrow(() -> new RuntimeException("Could not find role: " + role));
        String previousRole = target.getRoles().iterator().next().getName();
        target.getRoles().clear();
        target.getRoles().add(userRole);
        userRepository.save(target);
        mailService.sendRoleChangeMail(target, previousRole, role);
    }

    @Transactional
    public boolean deleteUser(AuthenticatedUser principal, Long userId, DeletionReason deletionReason) {
        User target = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean selfDelete = principal.getUserId().equals(userId);

        boolean isSuperAdmin = isSuperAdmin(principal);

        boolean targetIsSuperAdmin = isSuperAdmin(target);

        String reason = deletionReason.getReason();

        if (selfDelete) {
            softDeleteUser(target, principal.getUserId(), false, "self-deleted");
            return true;
        } else if (isSuperAdmin && !targetIsSuperAdmin) {
            if (reason != null && !reason.isBlank()) {
                softDeleteUser(target, principal.getUserId(), true, reason);
                return true;
            }  else {
                throw new IllegalArgumentException("Reason cannot be null or empty");
            }
        } else {
            return false;
        }
    }

    private void softDeleteUser(User user, Long actorId, boolean isAdmin, String reason) {

        // 1. Mark as deleted
        user.setDeletedAt(Instant.now());
        setUserStateInactive(user);
        userRepository.save(user);

        // 2. Audit the action
        UserDeletionLog log = new UserDeletionLog();
        log.setTargetUserId(user.getId());
        log.setActorId(actorId);
        log.setDeletionType(isAdmin ? "ADMIN" : "SELF");
        log.setReason(reason);
        logRepository.save(log);

        mailService.sendDeletionMail(user, isAdmin, reason);
    }

    @Transactional
    public void recoverAccount(String email, AuthenticatedUser principal) {
        User user = userRepository.findSoftDeletedByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Account not found or already purged"));

        // Restore the account
        user.setDeletedAt(null);
        setUserStateActive(user);
        userRepository.save(user);

        // Log the recovery
        UserRecoveryLog log = new UserRecoveryLog();
        log.setTargetUserId(user.getId());
        if (principal != null) {
            log.setRecoveredById(principal.getUserId());
        } else {
            log.setRecoveredById(user.getId());
        }

        recoveryLogRepository.save(log);

        boolean byAdmin = principal != null;

        mailService.sendRecoveryMail(user, byAdmin);
    }

    private static boolean isUser(User user) {
        return hasRole(user, ROLE_USER_STRING);
    }

    private static boolean isAdmin(User user) {
        return hasRole(user, ROLE_ADMIN_STRING);
    }

    private static boolean isAdmin(AuthenticatedUser principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> Objects.requireNonNull(a.getAuthority()).equalsIgnoreCase(ROLE_ADMIN_STRING));
    }

    private static boolean isSuperAdmin(User user) {
        return hasRole(user, ROLE_SUPER_ADMIN_STRING);
    }

    private static boolean isSuperAdmin(AuthenticatedUser principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> Objects.requireNonNull(a.getAuthority()).equalsIgnoreCase(ROLE_SUPER_ADMIN_STRING));
    }

    private static boolean hasRole(User user, String role) {
        return user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(role));
    }

    private void setUserStateInactive(User user) {
        UserState inactiveState = userStateRepository.findByName("INACTIVE").orElseThrow(() -> new RuntimeException("Could not find INACTIVE state"));
        user.setState(inactiveState);
    }

    private void setUserStateActive(User user) {
        UserState activeState = userStateRepository.findByName("ACTIVE").orElseThrow(() -> new RuntimeException("Could not find active state"));
        user.setState(activeState);
    }
}
