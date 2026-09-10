package com.hansana.helpdesk.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.user.dto.CreateUserRequest;
import com.hansana.helpdesk.user.dto.UpdateUserRequest;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Nested
    class GetUserByIdSecurityTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRoleReceives403() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRoleReceives403() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanGetUserById() throws Exception {
            UUID id = UUID.randomUUID();
            UserResponse user = new UserResponse(id, "Admin", "User", "admin@helpdesk.dev", UserRole.ADMIN);
            when(userService.getUserById(id)).thenReturn(user);

            mockMvc.perform(get("/api/v1/users/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.email").value("admin@helpdesk.dev"));
        }
    }

    @Nested
    class CreateUserEndpointSecurityTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            CreateUserRequest request = new CreateUserRequest("Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRoleReceives403() throws Exception {
            CreateUserRequest request = new CreateUserRequest("Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRoleReceives403() throws Exception {
            CreateUserRequest request = new CreateUserRequest("Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanCreateUser() throws Exception {
            CreateUserRequest request = new CreateUserRequest("Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);
            UUID id = UUID.randomUUID();
            UserResponse response = new UserResponse(id, "Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.email").value("jane@example.com"))
                    .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"));
        }
    }

    @Nested
    class UpdateUserEndpointSecurityTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Updated", "newemail@example.com");
            mockMvc.perform(patch("/api/v1/users/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRoleReceives403() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Updated", "newemail@example.com");
            mockMvc.perform(patch("/api/v1/users/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRoleReceives403() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Updated", "newemail@example.com");
            mockMvc.perform(patch("/api/v1/users/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanUpdateUser() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Updated", "newemail@example.com");
            UserResponse response = new UserResponse(id, "Jane", "Updated", "newemail@example.com", UserRole.SUPPORT_AGENT);

            when(userService.updateUser(eq(id), any(UpdateUserRequest.class))).thenReturn(response);

            mockMvc.perform(patch("/api/v1/users/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Updated"))
                    .andExpect(jsonPath("$.email").value("newemail@example.com"));
        }
    }

    @Nested
    class LifecycleEndpointSecurityTests {

        @Test
        void unauthenticatedActivateReturns401() throws Exception {
            mockMvc.perform(post("/api/v1/users/" + UUID.randomUUID() + "/activate"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRoleCannotActivate() throws Exception {
            mockMvc.perform(post("/api/v1/users/" + UUID.randomUUID() + "/activate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanActivate() throws Exception {
            UUID id = UUID.randomUUID();
            UserResponse response = new UserResponse(id, "Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);
            when(userService.activateUser(id)).thenReturn(response);

            mockMvc.perform(post("/api/v1/users/" + id + "/activate"))
                    .andExpect(status().isOk());
        }

        @Test
        void unauthenticatedDeactivateReturns401() throws Exception {
            mockMvc.perform(post("/api/v1/users/" + UUID.randomUUID() + "/deactivate"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRoleCannotDeactivate() throws Exception {
            mockMvc.perform(post("/api/v1/users/" + UUID.randomUUID() + "/deactivate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = "ADMIN")
        void adminCanDeactivate() throws Exception {
            UUID id = UUID.randomUUID();
            UserResponse response = new UserResponse(id, "Jane", "Doe", "jane@example.com", UserRole.SUPPORT_AGENT);
            when(userService.deactivateUser(eq(id), eq("admin@helpdesk.dev"))).thenReturn(response);

            mockMvc.perform(post("/api/v1/users/" + id + "/deactivate"))
                    .andExpect(status().isOk());
        }
    }
}
