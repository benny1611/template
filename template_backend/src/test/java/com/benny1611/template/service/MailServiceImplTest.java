package com.benny1611.template.service;

import com.benny1611.template.entity.User;
import com.benny1611.template.util.LocaleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MessageSource messageSource;
    @Mock private LocaleProvider localeProvider;

    @InjectMocks
    private MailServiceImpl mailService;

    private User testUser;
    private final String FROM_EMAIL = "noreply@template.com";
    private final String FRONTEND_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        // Manually set the @Value fields since there's no Spring Context
        ReflectionTestUtils.setField(mailService, "from", FROM_EMAIL);
        ReflectionTestUtils.setField(mailService, "frontendUrl", FRONTEND_URL);

        testUser = new User();
        testUser.setEmail("benny@example.com");
        testUser.setName("Benny");
        testUser.setLanguage("en");
    }

    @Test
    @DisplayName("Password Reset Email - Should construct correct link and content")
    void sendPasswordResetEmail_Success() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        String secret = "super-secret-code";

        when(localeProvider.supports(any())).thenReturn(true);
        when(messageSource.getMessage(eq("password.reset.subject"), any(), any()))
                .thenReturn("Reset Password");
        when(messageSource.getMessage(eq("password.reset.body"), any(), any()))
                .thenReturn("Hello, click here: " + FRONTEND_URL + "/reset-password");

        // Act
        mailService.sendPasswordResetEmail(testUser, tokenId, secret, 15);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertEquals(testUser.getEmail(), sentMessage.getTo()[0]);
        assertEquals("Reset Password", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(FRONTEND_URL + "/reset-password"));
    }

    @Test
    @DisplayName("Locale Resolution - Should fallback to English if language is unsupported")
    void resolveLocale_FallbackToEnglish() {
        // Arrange
        testUser.setLanguage("fr"); // User wants French
        when(localeProvider.supports(Locale.FRENCH)).thenReturn(false); // App doesn't support French

        // Mocking the messageSource to verify which locale was used
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Mock Text");

        // Act
        mailService.sendUnbanMail(testUser);

        // Assert
        // Capture the locale passed to the messageSource
        ArgumentCaptor<Locale> localeCaptor = ArgumentCaptor.forClass(Locale.class);
        verify(messageSource, times(2)).getMessage(anyString(), any(), localeCaptor.capture());

        assertEquals(Locale.ENGLISH, localeCaptor.getValue());
    }

    @Test
    @DisplayName("Deletion Mail - Should use admin template when byAdmin is true")
    void sendDeletionMail_ByAdmin() {
        // Arrange
        when(localeProvider.supports(any())).thenReturn(true);
        when(messageSource.getMessage(eq("delete.by_admin.subject"), any(), any()))
                .thenReturn("Account Terminated");

        // Act
        mailService.sendDeletionMail(testUser, true, "Violation of terms");

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertEquals("Account Terminated", messageCaptor.getValue().getSubject());
    }

    @Test
    @DisplayName("sendRecoveryMail - Self Recovery - Should use 'self' template")
    void sendRecoveryMail_Self_Success() {
        // Arrange
        when(localeProvider.supports(any())).thenReturn(true);
        when(messageSource.getMessage(eq("restore.self.subject"), any(), any()))
                .thenReturn("Account Restored");
        when(messageSource.getMessage(eq("restore.self.body"), any(), any()))
                .thenReturn("Hi Benny, your account is back.");

        // Act
        mailService.sendRecoveryMail(testUser, false); // byAdmin = false

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("Account Restored", sentMessage.getSubject());
        assertEquals("Hi Benny, your account is back.", sentMessage.getText());
    }

    @Test
    @DisplayName("sendRoleChangeMail - Should include both old and new roles in body")
    void sendRoleChangeMail_Success() {
        // Arrange
        String oldRole = "ROLE_USER";
        String newRole = "ROLE_ADMIN";

        when(localeProvider.supports(any())).thenReturn(true);
        when(messageSource.getMessage(eq("role.change.subject"), any(), any()))
                .thenReturn("Role Updated");

        // We mock the body to simulate the expected output of the messageSource arguments
        when(messageSource.getMessage(eq("role.change.body"), any(), any()))
                .thenReturn("Your role changed from ROLE_USER to ROLE_ADMIN");

        // Act
        mailService.sendRoleChangeMail(testUser, oldRole, newRole);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("Role Updated", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(oldRole));
        assertTrue(sentMessage.getText().contains(newRole));
    }


    @Test
    @DisplayName("sendDeletionMail - Self Deletion - Should use 'self' template and ignore reason")
    void sendDeletionMail_Self_Success() {
        // Arrange
        when(localeProvider.supports(any())).thenReturn(true);
        when(messageSource.getMessage(eq("delete.self.subject"), any(), any()))
                .thenReturn("Account Deleted");
        when(messageSource.getMessage(eq("delete.self.body"), any(), any()))
                .thenReturn("Goodbye Benny, your account has been removed.");

        // Act
        // Even if we pass a reason, the 'self' branch in your service doesn't use it
        mailService.sendDeletionMail(testUser, false, "I don't need this anymore");

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("Account Deleted", sentMessage.getSubject());
        assertEquals("Goodbye Benny, your account has been removed.", sentMessage.getText());

        // Specifically verify the correct key was called
        verify(messageSource).getMessage(eq("delete.self.subject"), any(), any());
    }
}
