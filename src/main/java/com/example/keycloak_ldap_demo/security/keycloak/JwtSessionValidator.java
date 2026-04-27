package com.example.keycloak_ldap_demo.security.keycloak;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtSessionValidator implements OAuth2TokenValidator<Jwt> {

    private final SessionBlacklistService blacklistService;

    public JwtSessionValidator(SessionBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sessionId = jwt.getClaimAsString("sid");

        if (sessionId != null && blacklistService.isRevoked(sessionId)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Session has been revoked", null)
            );
        }

        return OAuth2TokenValidatorResult.success();
    }
}