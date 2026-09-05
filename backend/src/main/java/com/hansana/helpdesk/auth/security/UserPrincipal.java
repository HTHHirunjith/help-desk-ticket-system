package com.hansana.helpdesk.auth.security;

import com.hansana.helpdesk.user.entity.UserRole;

import java.security.Principal;
import java.util.UUID;

public class UserPrincipal implements Principal {

    private final UUID id;
    private final String email;
    private final UserRole role;

    public UserPrincipal(UUID id, String email, UserRole role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String getName() {
        return email;
    }
}
