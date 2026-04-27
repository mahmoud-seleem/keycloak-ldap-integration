package com.example.keycloak_ldap_demo.security.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("LdapUserDetailsService")
public class LdapUserDetailsServiceImpl implements UserDetailsService {


    private final LdapTemplate ldapTemplate;

    @Value("${ldap.user.search-base}")
    private String userSearchBase;

    @Value("${ldap.group.search-base}")
    private String groupSearchBase;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    public LdapUserDetailsServiceImpl(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Step 1: find the user entry in LDAP
        List<String> userDns = ldapTemplate.search(
                LdapQueryBuilder.query()
                        .base(userSearchBase)
                        .where("uid").is(username),
                new AbstractContextMapper<String>() {
                    @Override
                    protected String doMapFromContext(DirContextOperations ctx) {
                        // return the full DN of the found entry
                        return ctx.getDn().toString();
                    }
                }
        );

        if (userDns.isEmpty()) {
            throw new UsernameNotFoundException("User not found in LDAP: " + username);
        }

        String relativeDn = userDns.get(0);

        System.out.println("******************** user relative dn = "+relativeDn);
        // Step 2: find what groups this user belongs to
        // We search for groups where 'member' contains this user's full DN
        String fullDn = relativeDn  + ","+ ldapBase;

        List<SimpleGrantedAuthority> authorities = ldapTemplate.search(
                LdapQueryBuilder.query()
                        .base(groupSearchBase)
                        .where("member").is(fullDn),
                new AbstractContextMapper<SimpleGrantedAuthority>() {
                    @Override
                    protected SimpleGrantedAuthority doMapFromContext(DirContextOperations ctx) {
                        // cn=ADMIN → ROLE_ADMIN
                        String groupCn = ctx.getStringAttribute("cn");
                        return new SimpleGrantedAuthority("ROLE_" + groupCn);
                    }
                }
        );

        // If no group found, give ROLE_USER as default
        if (authorities.isEmpty()) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Step 3: return a Spring UserDetails object
        // Password is "{ldap}" — tells Spring to authenticate against LDAP
        // not against a local password store
        return User.builder()
                .username(username)
                .password(null) // placeholder — real check is the LDAP bind
                .authorities(authorities)
                .build();
    }
}
