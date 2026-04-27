package com.example.keycloak_ldap_demo.dtos.responses.rest;

public class LoginResponse {
    private String token;

    public LoginResponse(String token) { this.token = token; }
    public String getToken() { return token; }
}
