package com.hansana.helpdesk.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hansana.helpdesk.auth.dto.ChangePasswordRequest;
import com.hansana.helpdesk.auth.dto.LoginRequest;
import com.hansana.helpdesk.auth.dto.LoginResponse;
import com.hansana.helpdesk.auth.dto.RegisterRequest;
import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.auth.service.AuthService;
import com.hansana.helpdesk.common.controller.HealthController;
import com.hansana.helpdesk.common.exception.EmailAlreadyExistsException;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, HealthController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void healthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void registerSucceedsPubliclyWith201() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "Smith", "alice@example.com", "Password123");
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(userId, "Alice", "Smith", "alice@example.com", UserRole.USER);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.firstName", is("Alice")))
                .andExpect(jsonPath("$.lastName", is("Smith")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist());
    }

    @Test
    void registerFailsValidationWith400OnBlankFields() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    void registerReturns409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("Bob", "Jones", "bob@example.com", "Password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("Email is already registered")));
    }

    @Test
    void loginSucceedsPubliclyWith200AndToken() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "Password123");
        UUID userId = UUID.randomUUID();
        UserResponse user = new UserResponse(userId, "Alice", "Smith", "alice@example.com", UserRole.USER);
        LoginResponse response = new LoginResponse("valid.jwt.token", user);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("valid.jwt.token")))
                .andExpect(jsonPath("$.user.email", is("alice@example.com")))
                .andExpect(jsonPath("$.user.role", is("USER")))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.password_hash").doesNotExist());
    }

    @Test
    void loginFailsWithGeneric401OnBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void getMeFailsWith401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));
    }

    @Test
    void getMeFailsWith401WhenTokenIsInvalidOrExpired() throws Exception {
        when(jwtService.validateToken("invalid.or.expired.token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.or.expired.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void getMeSucceedsWith200WhenBearerTokenIsValid() throws Exception {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(userId, "Alice", "Smith", "alice@example.com", UserRole.USER);

        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("alice@example.com");
        when(jwtService.extractRole(token)).thenReturn(UserRole.USER);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(authService.getCurrentUser("alice@example.com")).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.firstName", is("Alice")))
                .andExpect(jsonPath("$.lastName", is("Smith")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist());
    }

    @Test
    void arbitraryNonPublicEndpointRequiresAuthenticationAndReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));
    }

    @Test
    void changePasswordFailsWith401WhenUnauthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void changePasswordSucceedsWith200WhenAuthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass123");
        doNothing().when(authService).changePassword(any(), any());

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password changed successfully")));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void changePasswordFailsValidationWith400OnBlankOrShortPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("", "123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
