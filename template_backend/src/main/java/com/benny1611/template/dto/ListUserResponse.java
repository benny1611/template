package com.benny1611.template.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ListUserResponse {
    private Long id;
    private String name;
    private String email;
    private String profilePicture;
    private boolean active;
    private boolean isBanned;
    private List<String> roles;
    private boolean softDeleted;
    private Instant deletedAt;
}
