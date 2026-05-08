package com.benny1611.template.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserRequest {
    @NotBlank(message = "name must not be empty")
    private String name;

    @Email
    @NotBlank(message = "email must not be empty")
    private String email;

    @NotBlank(message = "password cannot be empty")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "Password must be at least 8 characters long and contain a letter, a number, and a special character"
    )
    private String password;

    @NotNull(message = "roles must be present")
    private Set<String> roles;
}
