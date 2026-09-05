package com.hansana.helpdesk.auth.controller;

import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoleTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class RoleAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    private void mockAuthenticatedUser(String token, UserRole role) {
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("user@example.com");
        when(jwtService.extractRole(token)).thenReturn(role);
        when(jwtService.extractUserId(token)).thenReturn(UUID.randomUUID());
    }

    // --- Unauthenticated Access (Must be 401 Unauthorized) ---

    @Test
    void unauthenticatedRequestToUserEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));
    }

    @Test
    void unauthenticatedRequestToAgentEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/agent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));
    }

    @Test
    void unauthenticatedRequestToAdminEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));
    }

    // --- USER Role Authorization ---

    @Test
    void userRoleCanAccessUserEndpoint() throws Exception {
        String token = "jwt.user.token";
        mockAuthenticatedUser(token, UserRole.USER);

        mockMvc.perform(get("/api/v1/test/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("USER access granted")));
    }

    @Test
    void userRoleIsForbiddenFromAgentEndpointWithAccessDeniedDetails() throws Exception {
        String token = "jwt.user.token";
        mockAuthenticatedUser(token, UserRole.USER);

        mockMvc.perform(get("/api/v1/test/agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access denied")))
                .andExpect(jsonPath("$.path", is("/api/v1/test/agent")));
    }

    @Test
    void userRoleIsForbiddenFromAdminEndpointWithAccessDeniedDetails() throws Exception {
        String token = "jwt.user.token";
        mockAuthenticatedUser(token, UserRole.USER);

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access denied")))
                .andExpect(jsonPath("$.path", is("/api/v1/test/admin")));
    }

    // --- SUPPORT_AGENT Role Authorization ---

    @Test
    void supportAgentCanAccessUserEndpoint() throws Exception {
        String token = "jwt.agent.token";
        mockAuthenticatedUser(token, UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/v1/test/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("USER access granted")));
    }

    @Test
    void supportAgentCanAccessAgentEndpoint() throws Exception {
        String token = "jwt.agent.token";
        mockAuthenticatedUser(token, UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/v1/test/agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("SUPPORT_AGENT access granted")));
    }

    @Test
    void supportAgentIsForbiddenFromAdminEndpointWithAccessDeniedDetails() throws Exception {
        String token = "jwt.agent.token";
        mockAuthenticatedUser(token, UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access denied")))
                .andExpect(jsonPath("$.path", is("/api/v1/test/admin")));
    }

    // --- ADMIN Role Authorization ---

    @Test
    void adminCanAccessUserEndpoint() throws Exception {
        String token = "jwt.admin.token";
        mockAuthenticatedUser(token, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/test/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("USER access granted")));
    }

    @Test
    void adminCanAccessAgentEndpoint() throws Exception {
        String token = "jwt.admin.token";
        mockAuthenticatedUser(token, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/test/agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("SUPPORT_AGENT access granted")));
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        String token = "jwt.admin.token";
        mockAuthenticatedUser(token, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("ADMIN access granted")));
    }

    // --- JWT Role Propagation & Security ---

    @Test
    void jwtRoleIsUsedForAuthorizationAndCannotBeOverriddenByClientBody() throws Exception {
        String userToken = "jwt.user.token";
        mockAuthenticatedUser(userToken, UserRole.USER);

        // Attempt to pass an admin role in body to gain agent/admin access
        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access denied")));
    }
}
