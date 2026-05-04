package com.example.freshkitchen.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class JwtAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final Role role;

    public JwtAuthentication(Long userId, Role role) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        this.userId = userId;
        this.role = role;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }
}
