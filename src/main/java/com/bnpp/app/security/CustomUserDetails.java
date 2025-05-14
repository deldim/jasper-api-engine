package com.bnpp.app.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.bnpp.app.model.ApiUser;

public class CustomUserDetails implements UserDetails {

    private final ApiUser user;

    public CustomUserDetails(ApiUser user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
            .map(role -> {
                return new SimpleGrantedAuthority(role);
            })
            .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public ApiUser getUser() {
        return user;
    }
}
