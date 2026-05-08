package com.benny1611.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class PasswordResetConfirmRequest {
    @NotBlank
    private String secret;

    @NotNull
    private UUID tokenId;

    @NotBlank(message = "password cannot be empty")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "Password must be at least 8 characters long and contain a letter, a number, and a special character"
    )
    private String newPassword;
}
