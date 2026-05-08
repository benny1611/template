package com.benny1611.template.controller;

import com.benny1611.template.auth.AuthenticatedUser;
import com.benny1611.template.dto.*;
import com.benny1611.template.entity.User;
import com.benny1611.template.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> createUser(@Valid @ModelAttribute CreateUserRequest request,
                                              @RequestPart(required = false) MultipartFile profilePicture) throws IOException {
        User user = userService.createUser(request, profilePicture);
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(user.getEmail());
        userDTO.setName(user.getName());
        userDTO.setProfilePicture(user.getProfilePictureUrl());
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> activateUser(@Valid @RequestBody ActivationMailRequest request) {
        User user = userService.activateUser(request.getToken());
        if (user != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<Void> resendActivation(@RequestParam @Email String email) {
        userService.resendActivation(email);
        return ResponseEntity.ok().build();
    }

    @PutMapping(
            value = "/update",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserDTO> updateUser(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestPart("userDTO") @Valid UserDTO userDTO,
            @RequestPart(value = "profilePicture", required = false)
            MultipartFile profilePicture
    ) throws IOException {
        UserDTO user = userService.updateUser(principal, userDTO, profilePicture);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/update/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListUserResponse> updateUserByAdmin(@PathVariable Long userId,
                                              @AuthenticationPrincipal AuthenticatedUser principal,
                                              @RequestPart("changeUserRequest") @Valid ChangeUserRequest changeUserRequest,
                                              @RequestPart(value = "profilePicture", required = false)
                                              MultipartFile profilePicture) throws IOException {
        ListUserResponse user = userService.updateUserByAdmin(principal, userId, changeUserRequest, profilePicture);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/update/admin/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ListUserResponse> updateUserBySuperAdmin(@PathVariable Long userId,
                                                          @AuthenticationPrincipal AuthenticatedUser principal,
                                                          @RequestPart("changeUserRequest") @Valid ChangeUserRequest changeUserRequest,
                                                          @RequestPart(value = "profilePicture", required = false)
                                                              MultipartFile profilePicture) throws IOException {
        ListUserResponse user = userService.updateUserBySuperAdmin(principal, userId, changeUserRequest, profilePicture);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/ban/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUserById(@PathVariable Long userId,
                                            @AuthenticationPrincipal AuthenticatedUser principal,
                                            @Valid @RequestBody BanRequest banRequest) {
        boolean bannedSuccessfully = userService.banUserById(principal, userId, banRequest);
        if (bannedSuccessfully) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/unban/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unbanUserById(@PathVariable Long userId,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        boolean unbannedSuccessfully = userService.unbanUserById(principal, userId);
        if (unbannedSuccessfully) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/update/roles/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserRole(@PathVariable Long userId,
                                               @AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestBody @Valid ChangeRolesRequest changeRolesRequest) {
        boolean changedUserRolesSuccessfully = userService.changeUserRoles(principal, userId, changeRolesRequest);
        if (changedUserRolesSuccessfully) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        UserDTO userDTO = userService.findById(principal);
        if (userDTO != null) {
            return new ResponseEntity<>(userDTO, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedModel<EntityModel<ListUserResponse>> getAllUsers(@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.DESC) Pageable pageable,
                                                        PagedResourcesAssembler<ListUserResponse> assembler) {
        Page<ListUserResponse> page = userService.getAllUsers(pageable);
        return assembler.toModel(page);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteUser(@PathVariable @NotNull Long userId,
                                           @AuthenticationPrincipal AuthenticatedUser principal,
                                           @RequestBody DeletionReason deletionReason) {
        boolean successfullyDeleted = userService.deleteUser(principal, userId, deletionReason);
        if (successfullyDeleted) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/recover")
    public ResponseEntity<Void> recoverUser(@RequestParam @Email String email,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        if (!email.isBlank()) {
            userService.recoverAccount(email, principal);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

}
