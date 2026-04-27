package com.example.keycloak_ldap_demo.security.ldap;

import com.example.keycloak_ldap_demo.dtos.requests.rest.RegisterationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;


import javax.lang.model.element.Name;
import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import java.util.List;
import java.util.logging.Logger;

@Service
public class LdapService {
    private final LdapTemplate ldapTemplate;

    @Value("${ldap.user.search-base}")
    private String userBase;         // ou=people

    @Value("${ldap.group.search-base}")
    private String groupBase;        // ou=groups

    @Value("${spring.ldap.base}")
    private String ldapBase;

    public LdapService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public List<String> getAllMatchingDnByUid(String uid){
        List<String> result = ldapTemplate.search(
                LdapQueryBuilder.query()
                        .base(userBase)
                        .where("uid").is(uid),
                new AbstractContextMapper<String>() {
                    @Override
                    protected String doMapFromContext(DirContextOperations ctx) {
                        // return the full DN of the found entry
                        return ctx.getDn().toString();
                    }
                }
        );


        System.out.println("found " + (result != null ? result.size() : 0) + " matching users with uid = " + uid);
        return result;
    }

    public List<String> getAllMatchingDnByEmail(String email){
        List<String> result = ldapTemplate.search(
                LdapQueryBuilder.query()
                        .base(userBase)
                        .where("mail").is(email),
                new AbstractContextMapper<String>() {
                    @Override
                    protected String doMapFromContext(DirContextOperations ctx) {
                        // return the full DN of the found entry
                        return ctx.getDn().toString();
                    }
                }
        );


        System.out.println("found " + (result != null ? result.size() : 0) + " matching users with email = " + email);
        return result;
    }

    public boolean userExists(String uid) {
        return !getAllMatchingDnByUid(uid).isEmpty();
    }

    public boolean mailExists(String email) {
        return !getAllMatchingDnByEmail(email).isEmpty();
    }
    public void createUser(RegisterationRequest request) {

        // Build the DN: cn=Ali,ou=people,dc=example,dc=com
        // LdapNameBuilder builds it cleanly
        LdapName userDn = LdapNameBuilder
                .newInstance(userBase)
                .add("uid", request.getUsername())
                .build();

        // Build the attributes for the new LDAP entry
        Attributes attrs = new BasicAttributes(true); // true = ignore case

        // objectClass values — we need all four for inetOrgPerson
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("organizationalPerson");
        objectClass.add("inetOrgPerson");
        attrs.put(objectClass);

        // Required attributes
        attrs.put("cn", request.getFirstName() + " " + request.getLastName());          // common name
        attrs.put("sn", request.getLastName());           // surname (required by 'person')
        attrs.put("uid", request.getUsername());          // login username

        // Optional but useful attributes
        if (request.getEmail() != null) {
            attrs.put("mail", request.getEmail());
        }

        // Password — LDAP stores it hashed
        // We prefix with {MD5} or {SSHA} to tell LDAP which algorithm to use
        // Using plain for now — in production use SSHA
        attrs.put("userPassword", request.getPassword());

        // Write the entry to LDAP
        try{

            ldapTemplate.bind(userDn, null, attrs);
        }catch (org.springframework.ldap.NameAlreadyBoundException e){
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
    }

    public void addUserToGroup(String userName, String groupName) {

        // Build the full DN of the user — this is what the group stores as 'member'
        String userFullDn = "uid=" + userName + "," + userBase + "," + ldapBase;

        // Build the DN of the group entry
        LdapName groupDn = LdapNameBuilder
                .newInstance(groupBase)
                .add("cn", groupName)
                .build();

        // A ModificationItem adds a new value to the 'member' attribute
        // DirContext.ADD_ATTRIBUTE = add a value, not replace
        ModificationItem[] mods = new ModificationItem[]{
                new ModificationItem(
                        DirContext.ADD_ATTRIBUTE,
                        new BasicAttribute("member", userFullDn)
                )
        };

        ldapTemplate.modifyAttributes(groupDn, mods);
    }

    public void deleteUser(String userName) {

        LdapName userDn = LdapNameBuilder
                .newInstance(userBase)
                .add("uid", userName)
                .build();

        ldapTemplate.unbind(userDn);
    }


}
