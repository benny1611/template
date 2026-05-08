package com.benny1611.template.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email
    @NotBlank(message = "email is missing or blank")
    private String email;

    @NotBlank(message = "password is missing or blank")
    private String password;
}
