package com.benny1611.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OauthCodeRequest {
    @NotBlank
    private String code;
}
