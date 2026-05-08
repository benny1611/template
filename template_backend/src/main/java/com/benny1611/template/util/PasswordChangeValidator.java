package com.benny1611.template.util;

import com.benny1611.template.dto.UserDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordChangeValidator implements ConstraintValidator<ValidUserDTOPasswordChange, UserDTO> {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$"
    );

    @Override
    public boolean isValid(UserDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        String oldPwd = dto.getOldPassword();
        String newPwd = dto.getNewPassword();

        if (oldPwd == null && newPwd == null) {
            return true;
        }

        if (oldPwd == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Old password must be present when trying to change password"
            ).addPropertyNode("newPassword").addConstraintViolation();
            return false;
        }

        if (newPwd == null || !PASSWORD_PATTERN.matcher(newPwd).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "New password must be at least 8 characters long and contain a letter, a number, and a special character"
            ).addPropertyNode("newPassword").addConstraintViolation();
            return false;
        }

        if(oldPwd.equals(newPwd)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "new password must be different than the old password"
            ).addPropertyNode("newPassword").addConstraintViolation();
            return false;
        }

        return true;
    }
}
