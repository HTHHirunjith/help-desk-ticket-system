package com.hansana.helpdesk.user.controller;

import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.user.dto.CreateUserRequest;
import com.hansana.helpdesk.user.dto.UpdateUserRequest;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(name = "role", required = false) UserRole role,
            @RequestParam(name = "active", required = false) Boolean active) {
        List<UserResponse> users = userService.findUsers(role, active);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID userId) {
        UserResponse user = userService.activateUser(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID userId, Principal principal) {
        String adminEmail = principal != null ? principal.getName() : null;
        UserResponse user = userService.deactivateUser(userId, adminEmail);
        return ResponseEntity.ok(user);
    }
}
