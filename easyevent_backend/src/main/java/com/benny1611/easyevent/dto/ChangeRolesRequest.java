package com.benny1611.easyevent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChangeRolesRequest {
    @NotNull
    @NotEmpty
    private List<String> roles;
}
