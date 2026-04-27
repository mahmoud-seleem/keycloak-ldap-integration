package com.example.keycloak_ldap_demo.security.ldap;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Component
public class LdapAuthenticationProvider implements AuthenticationProvider {


    private final LdapUserDetailsServiceImpl ldapUserDetailsService;

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${ldap.user.search-base}")
    private String userSearchBase;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    public LdapAuthenticationProvider(LdapUserDetailsServiceImpl ldapUserDetailsService) {
        this.ldapUserDetailsService = ldapUserDetailsService;
    }
    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Load user details (finds DN and groups from LDAP)
        UserDetails userDetails = ldapUserDetailsService.loadUserByUsername(username);
       userDetails.getAuthorities().stream().forEach((authority) -> System.out.println(authority.getAuthority()));
        // Construct the full DN to bind as
        String userDn = "uid=" + username + "," + userSearchBase + "," + ldapBase;

        System.out.println("****************** FULL USER DN : "+ userDn);
        // Attempt to bind to LDAP as the user — this verifies the password
        if (!verifyPasswordViaLdapBind(userDn, password)) {
            throw new BadCredentialsException("Invalid LDAP credentials for: " + username);
        }
        System.out.println("******************* Password is correct ****************");

        // If bind succeeded, return an authenticated token
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }


    private boolean verifyPasswordViaLdapBind(String userDn, String password) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            new InitialDirContext(env).close(); // if this doesn't throw, bind succeeded
            return true;
        } catch (Exception e) {
            System.out.println("********* exception happen when binding with password " + e.getMessage() + "\n ***************** the exception class is  : "+ e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
