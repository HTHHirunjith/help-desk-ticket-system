package com.hansana.helpdesk.auth.dto;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private boolean active;
    private boolean mustChangePassword;
    private Instant createdAt;
    private Instant updatedAt;

    public UserResponse() {
    }

    public UserResponse(UUID id, String firstName, String lastName, String email, UserRole role) {
        this(id, firstName, lastName, email, role, true, false, null, null);
    }

    public UserResponse(UUID id, String firstName, String lastName, String email, UserRole role,
                        boolean active, boolean mustChangePassword, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.isMustChangePassword(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
