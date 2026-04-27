package com.example.keycloak_ldap_demo.security.keycloak;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─────────────────────────────────────────────────────
    // Login — calls Keycloak token endpoint with username/password
    // Returns the full token response (access_token, refresh_token, etc.)
    // ─────────────────────────────────────────────────────
    public Map<String, Object> login(String username, String password) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // OAuth2 Resource Owner Password Credentials grant
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "password");
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("username",      username);
        body.add("password",      password);
        body.add("scope",         "openid");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

       try {
           ResponseEntity<Map> response = restTemplate.postForEntity(
                   tokenUrl, request, Map.class
           );

           return response.getBody();
       }catch (Exception e){
           Map<String,Object> map = new HashMap<>();
           map.put("couldn't login",null);
        return map;
       }
       }

    // ─────────────────────────────────────────────────────
    // Logout — calls Keycloak logout endpoint with refresh token
    // This invalidates the session on Keycloak's side
    // ─────────────────────────────────────────────────────
    public void logout(String refreshToken) {
        String logoutUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(logoutUrl, request, Void.class);
    }

    // ─────────────────────────────────────────────────────
    // Refresh — get a new access token using the refresh token
    // ─────────────────────────────────────────────────────
    public Map<String, Object> refresh(String refreshToken) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "refresh_token");
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl, request, Map.class
        );

        return response.getBody();
    }
}
