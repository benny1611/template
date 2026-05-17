package com.benny1611.template.service;

import com.benny1611.template.dao.UserRepository;
import com.benny1611.template.dto.OauthCodeRequest;
import com.benny1611.template.entity.User;
import com.benny1611.template.exception.AccountSoftDeletedException;
import com.benny1611.template.util.JwtUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OAuthService {

    private final OAuthCodeService codeService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    public OAuthService(OAuthCodeService codeService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.codeService = codeService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Transactional
    public String exchange(OauthCodeRequest request) {
        Long userID = codeService.consume(request.getCode());

        Session session = entityManager.unwrap(Session.class);

        // DISABLE the filter so we can see deleted users
        session.disableFilter("deletedUserFilter");
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("Could not find user: " + userID));

        if (user.getDeletedAt() != null) {
            throw new AccountSoftDeletedException(user.getEmail());
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return jwtUtils.generateToken(user);
    }
}
