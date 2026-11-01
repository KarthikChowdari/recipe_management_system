package com.recipes.rms.config;

import com.recipes.rms.domain.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Users bound from {@code app.security.users} in application.yml. */
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(List<UserEntry> users) {

    public AppSecurityProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record UserEntry(String username, String password, List<Role> roles) {
    }
}
