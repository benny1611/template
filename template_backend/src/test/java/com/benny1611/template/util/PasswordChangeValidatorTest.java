package com.benny1611.template.util;

import com.benny1611.template.dto.UserDTO;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeValidatorTest {

    @InjectMocks
    private PasswordChangeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        // This mocks the fluent API chain:
        // context.buildConstraintViolationWithTemplate().addPropertyNode().addConstraintViolation()
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        lenient().when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("Should be valid when DTO is null")
    void shouldBeValidWhenDtoIsNull() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("Should be valid when both passwords are null (no change attempted)")
    void shouldBeValidWhenBothPasswordsAreNull() {
        UserDTO dto = new UserDTO();
        dto.setOldPassword(null);
        dto.setNewPassword(null);

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("Should be invalid when only new password is provided")
    void shouldBeInvalidWhenOldPasswordMissing() {
        UserDTO dto = new UserDTO();
        dto.setOldPassword(null);
        dto.setNewPassword("SecureP@ss123");

        assertThat(validator.isValid(dto, context)).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Old password must be present when trying to change password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "noNumbers!", "noSpecialChar1", "12345678!"})
    @DisplayName("Should be invalid when new password doesn't meet complexity")
    void shouldBeInvalidWhenNewPasswordIsWeak(String weakPassword) {
        UserDTO dto = new UserDTO();
        dto.setOldPassword("CurrentP@ss1");
        dto.setNewPassword(weakPassword);

        assertThat(validator.isValid(dto, context)).isFalse();
        verify(context).buildConstraintViolationWithTemplate(contains("New password must be at least 8 characters long"));
    }

    @Test
    @DisplayName("Should be invalid when new password is same as old")
    void shouldBeInvalidWhenPasswordsAreSame() {
        String samePass = "SameP@ss123!";
        UserDTO dto = new UserDTO();
        dto.setOldPassword(samePass);
        dto.setNewPassword(samePass);

        assertThat(validator.isValid(dto, context)).isFalse();
        verify(context).buildConstraintViolationWithTemplate("new password must be different than the old password");
    }

    @Test
    @DisplayName("Should be valid when all conditions are met")
    void shouldBeValidOnCorrectPasswordChange() {
        UserDTO dto = new UserDTO();
        dto.setOldPassword("OldP@ss123");
        dto.setNewPassword("NewP@ss456!");

        assertThat(validator.isValid(dto, context)).isTrue();
    }
}