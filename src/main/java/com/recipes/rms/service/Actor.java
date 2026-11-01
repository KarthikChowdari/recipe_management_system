package com.recipes.rms.service;

import com.recipes.rms.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** The user performing an operation; decouples services from Spring Security types. */
public record Actor(String username, Set<Role> roles) {

    private static final String ROLE_PREFIX = "ROLE_";

    public Actor {
        roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    }

    public static Actor of(String username, Role... roles) {
        return new Actor(username, Set.copyOf(Arrays.asList(roles)));
    }

    public static Actor from(Authentication authentication) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith(ROLE_PREFIX)) {
                try {
                    roles.add(Role.valueOf(name.substring(ROLE_PREFIX.length())));
                } catch (IllegalArgumentException ignored) {
                    // authority not known to this application
                }
            }
        }
        return new Actor(authentication.getName(), roles);
    }

    public boolean has(Role role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return has(Role.ADMIN);
    }
}
