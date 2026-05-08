package com.benny1611.template.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ChangeUserRequest {
    @Email
    private String email;
    private String name;
}
