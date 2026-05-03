package com.benny1611.easyevent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BanRequest {
    @NotBlank
    @NotNull
    private String reason;
}
