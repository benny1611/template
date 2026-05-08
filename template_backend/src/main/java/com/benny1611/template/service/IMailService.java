package com.benny1611.template.service;

import com.benny1611.template.entity.User;

import java.util.UUID;

public interface IMailService {
    void sendPasswordResetEmail(User user, UUID tokenId, String secret, int expiryMinutes);
    void sendActivationEmail(User user);
    void sendBanMail(User user, String reason);
    void sendUnbanMail(User user);
    void sendRoleChangeMail(User user, String previousRole, String newRole);
    void sendDeletionMail(User user, boolean byAdmin, String reason);
    void sendRecoveryMail(User user, boolean byAdmin);
}
