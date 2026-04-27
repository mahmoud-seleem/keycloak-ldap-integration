package com.example.keycloak_ldap_demo.dtos.requests.rest;


public class RegisterationRequest {
    private String username;   // becomes uid
    private String password;
    private String firstName;  // becomes cn
    private String lastName;   // becomes sn
    private String email;      // becomes mail
    private String role;
    // getters and setters
    public String getUsername()              { return username; }
    public String getPassword()              { return password; }
    public String getFirstName()             { return firstName; }
    public String getLastName()              { return lastName; }
    public String getEmail()                 { return email; }
    public void setUsername(String u)        { this.username = u; }
    public void setPassword(String p)        { this.password = p; }
    public void setFirstName(String f)       { this.firstName = f; }
    public void setLastName(String l)        { this.lastName = l; }
    public void setEmail(String e)           { this.email = e; }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
