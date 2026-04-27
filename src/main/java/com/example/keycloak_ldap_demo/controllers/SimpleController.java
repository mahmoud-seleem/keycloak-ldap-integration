package com.example.keycloak_ldap_demo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class SimpleController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

}
