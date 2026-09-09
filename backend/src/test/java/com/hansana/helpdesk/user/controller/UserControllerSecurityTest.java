package com.hansana.helpdesk.user.controller;

import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Nested
    class ListUsersEndpointSecurityTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRoleReceives403() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRoleReceives403() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanListUsersWithFilters() throws Exception {
            UserResponse agent = new UserResponse(
                    UUID.randomUUID(),
                    "Support",
                    "Agent",
                    "agent@helpdesk.dev",
                    UserRole.SUPPORT_AGENT
            );

            when(userService.findUsers(eq(UserRole.SUPPORT_AGENT), eq(true)))
                    .thenReturn(List.of(agent));

            mockMvc.perform(get("/api/v1/users")
                            .param("role", "SUPPORT_AGENT")
                            .param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("Support"))
                    .andExpect(jsonPath("$[0].lastName").value("Agent"))
                    .andExpect(jsonPath("$[0].email").value("agent@helpdesk.dev"))
                    .andExpect(jsonPath("$[0].role").value("SUPPORT_AGENT"));
        }
    }
}
