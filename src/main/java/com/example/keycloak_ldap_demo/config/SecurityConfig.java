package com.example.keycloak_ldap_demo.config;

import com.example.keycloak_ldap_demo.security.JwtFilter;
import com.example.keycloak_ldap_demo.security.keycloak.JwtRoleConverter;
import com.example.keycloak_ldap_demo.security.keycloak.JwtSessionValidator;
import com.example.keycloak_ldap_demo.security.ldap.LdapAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

import static org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration.jwtDecoder;

@Configuration
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    private final JwtSessionValidator sessionValidator;


    public SecurityConfig(JwtFilter jwtFilter, LdapAuthenticationProvider ldapAuthenticationProvider, JwtSessionValidator sessionValidator) {
        this.jwtFilter = jwtFilter;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
        this.sessionValidator = sessionValidator;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())           // disable CSRF (not needed for stateless APIs)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // no sessions
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()          // login & register are public
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN") // admin only
                        .requestMatchers("/api/user/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .anyRequest().authenticated()                     // everything else needs login
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // hashes passwords before storing
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        // Use our LDAP provider instead of the DB provider
        return new ProviderManager(List.of(ldapAuthenticationProvider));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // Chain the standard issuer validator with our session validator
        OAuth2TokenValidator<Jwt> withIssuer =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> withSession =
                new DelegatingOAuth2TokenValidator<>(withIssuer, sessionValidator);

        decoder.setJwtValidator(withSession);
        return decoder;
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // tell Spring to use our custom role converter
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }
}
