package com.benny1611.template.controller;

import com.benny1611.template.dto.LoginResponse;
import com.benny1611.template.dto.OauthCodeRequest;
import com.benny1611.template.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthService oAuthService;

    @Autowired
    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @PostMapping("/exchange")
    public ResponseEntity<LoginResponse> exchange(@Valid @RequestBody OauthCodeRequest request) {
        String token = oAuthService.exchange(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
