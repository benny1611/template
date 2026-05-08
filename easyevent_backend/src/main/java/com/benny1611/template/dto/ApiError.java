package com.benny1611.template.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ApiError {
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Instant timestamp = Instant.now();
}
