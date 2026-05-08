package com.benny1611.template.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @Email
    @NotBlank(message = "You must provide an email")
    private String email;
}
