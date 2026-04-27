package com.example.keycloak_ldap_demo.controllers;

import com.example.keycloak_ldap_demo.dtos.requests.rest.LoginRequest;
import com.example.keycloak_ldap_demo.dtos.requests.rest.RegisterationRequest;
import com.example.keycloak_ldap_demo.dtos.responses.rest.LoginResponse;
import com.example.keycloak_ldap_demo.model.Role;
import com.example.keycloak_ldap_demo.model.User;
import com.example.keycloak_ldap_demo.repository.UserRepository;
import com.example.keycloak_ldap_demo.security.JwtUtil;
import com.example.keycloak_ldap_demo.security.keycloak.KeycloakService;
import com.example.keycloak_ldap_demo.security.keycloak.LogoutTokenDecoder;
import com.example.keycloak_ldap_demo.security.keycloak.SessionBlacklistService;
import com.example.keycloak_ldap_demo.security.ldap.LdapService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LdapService ldapService;
    private final KeycloakService keycloakService;
    private final JwtDecoder jwtDecoder;
    private final SessionBlacklistService sessionBlacklistService;
    private final LogoutTokenDecoder logoutTokenDecoder;
    public AuthenticationController(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    AuthenticationManager authenticationManager,
                                    JwtUtil jwtUtil, LdapService ldapService, KeycloakService keycloakService, JwtDecoder jwtDecoder, SessionBlacklistService sessionBlacklistService, LogoutTokenDecoder logoutTokenDecoder) {
        this.userRepository       = userRepository;
        this.passwordEncoder      = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil              = jwtUtil;
        this.ldapService = ldapService;
        this.keycloakService = keycloakService;
        this.jwtDecoder = jwtDecoder;
        this.sessionBlacklistService = sessionBlacklistService;
        this.logoutTokenDecoder = logoutTokenDecoder;
    }


    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterationRequest request) {

        // validate required fields
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            return ResponseEntity.badRequest().body("First name is required (used as LDAP cn)");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            return ResponseEntity.badRequest().body("Last name is required (used as LDAP sn)");
        }
        if (ldapService.userExists(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already taken");
        }
        if (ldapService.mailExists(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        ldapService.createUser(request);

        ldapService.addUserToGroup(
                request.getFirstName(),
                request.getRole()
        );

        return ResponseEntity.ok("User registered successfully. " +
                "You can now log in with username: " + request.getUsername());
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestBody LoginRequest request) {
        // This throws an exception automatically if credentials are wrong
//        Authentication auth = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        request.getUsername(),
//                        request.getPassword()
//                )
//        );
//
//
//        UserDetails user = (UserDetails) auth.getPrincipal();
//        System.out.println("authenticated user with username = "+user.getUsername());
//        String token = jwtUtil.generateToken(user.getUsername(),user.getAuthorities().iterator().next().getAuthority());

        Map<String, Object> tokenResponse = keycloakService.login(
                request.getUsername(),
                request.getPassword()
        );
        return ResponseEntity.ok(tokenResponse);
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        keycloakService.logout(refreshToken);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        Map<String, Object> tokenResponse = keycloakService.refresh(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/backchannel-logout")
    public ResponseEntity<Void> backchannelLogout(
            @RequestParam("logout_token") String logoutToken) {
        try {
            Jwt jwt = logoutTokenDecoder.decode(logoutToken);
            String sessionId = jwt.getClaimAsString("sid");
            System.out.println(">>> Session revoked: " + sessionId);
            if (sessionId != null) {
                sessionBlacklistService.revokeSession(sessionId);
            }
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // print the FULL stack trace not just the message
            e.printStackTrace();
            System.out.println(">>> Backchannel decode failed: " + e.getMessage());
            System.out.println(">>> Exception type: " + e.getClass().getName());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/test/{uid}")
    public ResponseEntity<Boolean> test(@PathVariable  String uid){
        return ResponseEntity.ok(ldapService.userExists(uid));
    }
}

