package com.recipes.rms.domain;

/** Application roles. Spring Security authorities are these names prefixed with {@code ROLE_}. */
public enum Role {
    AUTHOR,
    REVIEWER,
    ADMIN
}
