package com.hansana.helpdesk.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.dashboard.dto.AdminDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.AgentDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.CategoryDistributionItem;
import com.hansana.helpdesk.dashboard.dto.UserDashboardResponse;
import com.hansana.helpdesk.dashboard.service.DashboardService;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.user.entity.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @Nested
    class UserDashboardEndpoint {

        @Test
        @WithMockUser(roles = "USER")
        void userRole_CanAccess_UserDashboard() throws Exception {
            UserDashboardResponse response = new UserDashboardResponse(12, 3, 2, 1, 6);
            when(dashboardService.getUserDashboard(any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/dashboard/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets", is(12)))
                    .andExpect(jsonPath("$.openTickets", is(3)))
                    .andExpect(jsonPath("$.inProgressTickets", is(2)))
                    .andExpect(jsonPath("$.resolvedTickets", is(1)))
                    .andExpect(jsonPath("$.closedTickets", is(6)));
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRole_CannotAccess_UserDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/user"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminRole_CannotAccess_UserDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/user"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticated_CannotAccess_UserDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/user"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AgentDashboardEndpoint {

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRole_CanAccess_AgentDashboard() throws Exception {
            AgentDashboardResponse response = new AgentDashboardResponse(8, 3, 2, 2, 1);
            when(dashboardService.getAgentDashboard(any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/dashboard/agent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTickets", is(8)))
                    .andExpect(jsonPath("$.openTickets", is(3)))
                    .andExpect(jsonPath("$.inProgressTickets", is(2)))
                    .andExpect(jsonPath("$.resolvedTickets", is(2)))
                    .andExpect(jsonPath("$.closedTickets", is(1)));
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRole_CannotAccess_AgentDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/agent"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminRole_CannotAccess_AgentDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/agent"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticated_CannotAccess_AgentDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/agent"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AdminDashboardEndpoint {

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminRole_CanAccess_AdminDashboard() throws Exception {
            UUID catId = UUID.randomUUID();
            AdminDashboardResponse response = new AdminDashboardResponse(
                    42, 12, 10, 8, 12, 5,
                    Map.of(
                            TicketPriority.LOW, 8L,
                            TicketPriority.MEDIUM, 18L,
                            TicketPriority.HIGH, 10L,
                            TicketPriority.URGENT, 6L
                    ),
                    List.of(new CategoryDistributionItem(catId, "ACCOUNT", 10L))
            );
            when(dashboardService.getAdminDashboard()).thenReturn(response);

            mockMvc.perform(get("/api/v1/dashboard/admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets", is(42)))
                    .andExpect(jsonPath("$.openTickets", is(12)))
                    .andExpect(jsonPath("$.inProgressTickets", is(10)))
                    .andExpect(jsonPath("$.resolvedTickets", is(8)))
                    .andExpect(jsonPath("$.closedTickets", is(12)))
                    .andExpect(jsonPath("$.unassignedTickets", is(5)))
                    .andExpect(jsonPath("$.priorityDistribution.LOW", is(8)))
                    .andExpect(jsonPath("$.priorityDistribution.MEDIUM", is(18)))
                    .andExpect(jsonPath("$.priorityDistribution.HIGH", is(10)))
                    .andExpect(jsonPath("$.priorityDistribution.URGENT", is(6)))
                    .andExpect(jsonPath("$.categoryDistribution[0].categoryName", is("ACCOUNT")))
                    .andExpect(jsonPath("$.categoryDistribution[0].ticketCount", is(10)));
        }

        @Test
        @WithMockUser(roles = "USER")
        void userRole_CannotAccess_AdminDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/admin"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentRole_CannotAccess_AdminDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/admin"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticated_CannotAccess_AdminDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/admin"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
