package com.hansana.helpdesk.auth.service;

import com.hansana.helpdesk.auth.dto.LoginRequest;
import com.hansana.helpdesk.auth.dto.LoginResponse;
import com.hansana.helpdesk.auth.dto.RegisterRequest;
import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.common.exception.EmailAlreadyExistsException;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerSuccessfullyCreatesUserWithUserRoleAndHashedPassword() {
        RegisterRequest request = new RegisterRequest("Alice", "Smith", "alice@example.com", "PlainSecret123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("PlainSecret123")).thenReturn("$2a$10$hashedPasswordValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals(UserRole.USER, response.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();

        assertEquals("alice@example.com", captured.getEmail());
        assertEquals(UserRole.USER, captured.getRole());
        assertTrue(captured.isActive());
        assertEquals("$2a$10$hashedPasswordValue", captured.getPassword());
        assertNotEquals("PlainSecret123", captured.getPassword());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Bob", "Jones", "bob@example.com", "SecretPass123");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "PlainSecret123");

        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("alice@example.com");
        existingUser.setFirstName("Alice");
        existingUser.setLastName("Smith");
        existingUser.setPassword("$2a$10$hashedPasswordValue");
        existingUser.setRole(UserRole.USER);
        existingUser.setActive(true);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("PlainSecret123", "$2a$10$hashedPasswordValue")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
        assertEquals(UserRole.USER, response.getUser().getRole());
    }

    @Test
    void loginFailsWithGeneric401WhenEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "AnyPassword");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginFailsWithGeneric401WhenPasswordIncorrect() {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword");

        User existingUser = new User();
        existingUser.setEmail("alice@example.com");
        existingUser.setPassword("$2a$10$hashedPasswordValue");
        existingUser.setActive(true);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPasswordValue")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginFailsWithGeneric401WhenUserIsInactive() {
        LoginRequest request = new LoginRequest("inactive@example.com", "PlainSecret123");

        User inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPassword("$2a$10$hashedPasswordValue");
        inactiveUser.setActive(false);

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void getCurrentUserReturnsUserResponse() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setRole(UserRole.USER);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser("alice@example.com");

        assertNotNull(response);
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals(UserRole.USER, response.getRole());
    }

    @Test
    void changePassword_withValidCurrentPassword_updatesPasswordAndClearsMustChangePassword() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@helpdesk.dev");
        user.setPassword("$2a$10$oldHash");
        user.setMustChangePassword(true);

        when(userRepository.findByEmail("agent@helpdesk.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("TempPass123", "$2a$10$oldHash")).thenReturn(true);
        when(passwordEncoder.encode("PermanentPass123")).thenReturn("$2a$10$newHash");

        authService.changePassword("agent@helpdesk.dev", new com.hansana.helpdesk.auth.dto.ChangePasswordRequest("TempPass123", "PermanentPass123"));

        assertEquals("$2a$10$newHash", user.getPassword());
        org.junit.jupiter.api.Assertions.assertFalse(user.isMustChangePassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_withInvalidCurrentPassword_throwsBadCredentialsException() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@helpdesk.dev");
        user.setPassword("$2a$10$oldHash");

        when(userRepository.findByEmail("agent@helpdesk.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "$2a$10$oldHash")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.changePassword("agent@helpdesk.dev", new com.hansana.helpdesk.auth.dto.ChangePasswordRequest("WrongPass", "NewPass123")));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
