package com.example.keycloak_ldap_demo.security.keycloak;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class LogoutTokenDecoder {

    private final NimbusJwtDecoder decoder;

    public LogoutTokenDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuerUri) {

        String jwksUri = issuerUri + "/protocol/openid-connect/certs";

        this.decoder = NimbusJwtDecoder
                .withJwkSetUri(jwksUri)
                // Tell Nimbus to accept both "JWT" and "logout+jwt" as valid typ values
                .jwtProcessorCustomizer(processor ->
                        processor.setJWSTypeVerifier(
                                new DefaultJOSEObjectTypeVerifier<>(
                                        new JOSEObjectType("logout+jwt"),  // ← Keycloak logout token type
                                        new JOSEObjectType("JWT"),          // ← standard access token type
                                        JOSEObjectType.JWT,                 // ← null typ (also valid)
                                        null
                                )
                        )
                )
                .build();

        // Accept everything that passes signature verification
        // Logout tokens have different claims than access tokens
        // so we skip all claim validation
        this.decoder.setJwtValidator(
                token -> org.springframework.security.oauth2.core
                        .OAuth2TokenValidatorResult.success()
        );
    }

    public Jwt decode(String token) {
        System.out.println(">>> LogoutTokenDecoder: decoding logout token");
        Jwt jwt = decoder.decode(token);
        System.out.println(">>> typ  = " + jwt.getHeaders().get("typ"));
        System.out.println(">>> iss  = " + jwt.getClaimAsString("iss"));
        System.out.println(">>> sid  = " + jwt.getClaimAsString("sid"));
        System.out.println(">>> events = " + jwt.getClaims().get("events"));
        return jwt;
    }
}