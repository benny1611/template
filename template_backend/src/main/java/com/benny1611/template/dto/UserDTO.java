package com.benny1611.template.dto;

import com.benny1611.template.util.ValidUserDTOPasswordChange;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Objects;

@Data
@ValidUserDTOPasswordChange
public class UserDTO {
    @NotNull
    private Long id;
    private String profilePicture;
    private String name;
    @Email
    private String email;
    private String language;
    private String oldPassword;
    private String newPassword;
    private String token;
    private boolean isLocalPasswordSet;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(id, userDTO.id);
    }
}
