package com.benny1611.template.service;

import com.benny1611.template.entity.User;
import com.benny1611.template.util.LocaleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class MailServiceImpl implements IMailService {

    private final JavaMailSender mailSender;
    private final MessageSource mailMessageSource;
    private final LocaleProvider localeProvider;


    @Value("${app.mail.from}")
    private String from;
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public MailServiceImpl(JavaMailSender mailSender, MessageSource mailMessageSource, LocaleProvider localeProvider) {
        this.mailSender = mailSender;
        this.mailMessageSource = mailMessageSource;
        this.localeProvider = localeProvider;
    }

    @Override
    @Async
    public void sendPasswordResetEmail(User user, UUID tokenId, String secret, int expiryMinutes) {
        Locale locale = resolveLocale(user);

        String resetLink =
                frontendUrl + "/reset-password?id=" + tokenId + "&token=" + secret;

        String subject = mailMessageSource.getMessage(
                "password.reset.subject",
                null,
                locale
        );

        String body = mailMessageSource.getMessage(
                "password.reset.body",
                new Object[]{user.getName(), resetLink, expiryMinutes},
                locale
        );

        sendMail(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendActivationEmail(User user) {
        Locale locale = resolveLocale(user);

        String subject = mailMessageSource.getMessage(
                "activation.subject",
                null,
                locale
        );

        String activationLink = frontendUrl + "/activate?token=" + user.getActivationToken();

        String body = mailMessageSource.getMessage(
                "activation.body",
                new Object[]{user.getName(), activationLink},
                locale
        );

        sendMail(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendBanMail(User user, String reason) {
        Locale locale = resolveLocale(user);

        String subject = mailMessageSource.getMessage(
                "ban.subject",
                null,
                locale
        );

        String body = mailMessageSource.getMessage(
                "ban.body",
                new Object[]{user.getName(), reason},
                locale
        );

        sendMail(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendUnbanMail(User user) {
        Locale locale = resolveLocale(user);

        String subject = mailMessageSource.getMessage(
                "unban.subject",
                null,
                locale
        );

        String body = mailMessageSource.getMessage(
                "unban.body",
                new Object[]{user.getName()},
                locale
        );

        sendMail(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendRoleChangeMail(User user, String previousRole, String newRole) {
        Locale locale = resolveLocale(user);

        String subject = mailMessageSource.getMessage(
                "role.change.subject",
                null,
                locale
        );

        String body = mailMessageSource.getMessage(
                "role.change.body",
                new Object[]{user.getName(), previousRole, newRole},
                locale
        );

        sendMail(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendDeletionMail(User user, boolean byAdmin, String reason) {
        Locale locale = resolveLocale(user);

        String subject;
        String body;

        if (byAdmin) {
            subject = mailMessageSource.getMessage(
                    "delete.by_admin.subject",
                    null,
                    locale
            );

            body = mailMessageSource.getMessage(
                    "delete.by_admin.body",
                    new Object[]{user.getName(), reason},
                    locale
            );
        } else {
            subject = mailMessageSource.getMessage(
                    "delete.self.subject",
                    null,
                    locale
            );

            body = mailMessageSource.getMessage(
                    "delete.self.body",
                    new Object[]{user.getName()},
                    locale
            );
        }

        sendMail(user.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendRecoveryMail(User user, boolean byAdmin) {
        Locale locale = resolveLocale(user);

        String subject;
        String body;

        if (byAdmin) {
            subject = mailMessageSource.getMessage(
                    "restore.by_admin.subject",
                    null,
                    locale
            );

            body = mailMessageSource.getMessage(
                    "restore.by_admin.body",
                    new Object[]{user.getName()},
                    locale
            );
        } else {
            subject = mailMessageSource.getMessage(
                    "restore.self.subject",
                    null,
                    locale
            );

            body = mailMessageSource.getMessage(
                    "restore.self.body",
                    new Object[]{user.getName()},
                    locale
            );
        }

        sendMail(user.getEmail(), subject, body);
    }

    private void sendMail(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    private Locale resolveLocale(User user) {
        if (user.getLanguage() == null) {
            return Locale.ENGLISH;
        }

        Locale requested = Locale.forLanguageTag(user.getLanguage());

        return localeProvider.supports(requested)
                ? requested
                : Locale.ENGLISH;
    }
}
