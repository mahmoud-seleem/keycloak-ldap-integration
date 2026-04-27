# Keycloak LDAP Demo

A Spring Boot application demonstrating **dual authentication** using **Keycloak** (OAuth2/OIDC) and **LDAP** for enterprise user management.

## Features

- **Keycloak Integration** — OAuth2/OIDC resource server with JWT validation
- **LDAP Authentication** — User authentication and group synchronization via LDAP
- **Role-Based Access Control** — JWT role conversion with Keycloak role mapping
- **Session Management** — Session blacklist for logout handling
- **PostgreSQL Database** — Persistent storage for user data

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- Keycloak (latest)
- PostgreSQL 16
- LDAP (OpenLDAP via Docker)

## Quick Start

```bash
# Start all services
docker-compose up -d

# Run the Spring Boot application
./mvnw spring-boot:run
```

## Access Points

| Service | URL |
|---------|-----|
| Keycloak Admin Console | http://localhost:9090 (admin/admin) |
| Sample API Endpoints | http://localhost:8080/api/... |

## Project Structure

```
src/main/java/com/example/keycloak_ldap_demo/
├── config/          # Security configuration
├── controllers/     # REST API endpoints
├── dtos/            # Request/Response objects
├── model/           # JPA entities
├── repository/      # Data access layer
└── security/
    ├── keycloak/    # Keycloak JWT handling
    └── ldap/        # LDAP authentication
```

## Overview

This demo shows how to combine Keycloak's OAuth2 capabilities with LDAP's enterprise user directory in a single Spring Boot application.

## Architecture Diagram

```mermaid
flowchart TB
    subgraph Client["Client Applications"]
        Browser["Browser / Frontend"]
        MobileApp["Mobile App"]
        APIClient["API Client"]
    end

    subgraph Keycloak["Keycloak Server"]
        KC["OAuth2/OIDC Provider"]
        KCAdmin["Admin Console\n:9090"]
    end

    subgraph LDAPServer["LDAP Directory"]
        LDAP["OpenLDAP\nUser Directory"]
        Users["Users & Groups"]
    end

    subgraph SpringBoot["Spring Boot Application :8080"]
        SecurityConfig["Security Config"]
        
        subgraph Auth["Authentication Layer"]
            JWTFilter["JWT Filter"]
            LdapAuth["LDAP Auth Provider"]
            KeycloakAuth["Keycloak Auth"]
        end
        
        subgraph Controllers["REST Controllers"]
            AuthController["/api/auth/*"]
            AdminController["/api/admin/*"]
            SimpleController["/api/*"]
        end
        
        subgraph Data["Data Layer"]
            UserRepo["User Repository"]
            DB["PostgreSQL Database"]
        end
    end

    %% Authentication Flows
    Browser -->|1. Login Request| AuthController
    AuthController -->|2. Authenticate| LdapAuth
    LdapAuth -->|3. Query Users| LDAPServer
    LDAPServer -->|4. Return User Data| LdapAuth
    LdapAuth -->|5. JWT Token| Browser
    
    Browser -->|6. API Request + JWT| JWTFilter
    JWTFilter -->|7. Validate Token| KC
    KC -->|8. Token Claims| JWTFilter
    JWTFilter -->|9. Authorized| Controllers
    
    %% Keycloak to LDAP sync
    KC -.->|User Federation| LDAPServer
    
    %% Database
    UserRepo -->|Read/Write| DB
    
    %% Styling
    classDef primary fill:#e1f5fe,stroke:#01579b,stroke-width:2px;
    classDef secondary fill:#f3e5f5,stroke:#4a148c,stroke-width:2px;
    classDef database fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px;
    classDef external fill:#fff3e0,stroke:#e65100,stroke-width:2px;
    
    class Browser,MobileApp,APIClient primary;
    class KC,KCAdmin,LDAPServer external;
    class SecurityConfig,AuthController,AdminController,SimpleController secondary;
    class UserRepo,DB database;
```

### Flow Explanation

| Step | Description |
|------|-------------|
| **1-5** | **LDAP Authentication Flow**: Client sends login → Spring Boot queries LDAP directory → Returns JWT token |
| **6-9** | **Keycloak JWT Validation**: Client sends API request with JWT → Spring Boot validates token with Keycloak → Authorizes request |