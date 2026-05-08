package com.benny1611.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ActivationMailRequest {

    @NotNull(message = "token should not be null")
    private UUID token;
}
