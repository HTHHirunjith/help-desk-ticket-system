package com.hansana.helpdesk.dashboard.service;

import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.dashboard.dto.AdminDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.AgentDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.UserDashboardResponse;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import com.hansana.helpdesk.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(ticketRepository, categoryRepository);
    }

    @Nested
    class UserDashboardTests {

        @Test
        void shouldReturnUserDashboardWithCorrectCounts() {
            UUID userId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(userId, "user@example.com", UserRole.USER);

            when(ticketRepository.countByRequesterId(userId)).thenReturn(5L);
            List<Object[]> statusCounts = new ArrayList<>();
            statusCounts.add(new Object[]{TicketStatus.OPEN, 2L});
            statusCounts.add(new Object[]{TicketStatus.IN_PROGRESS, 1L});
            statusCounts.add(new Object[]{TicketStatus.RESOLVED, 1L});
            statusCounts.add(new Object[]{TicketStatus.CLOSED, 1L});
            when(ticketRepository.countByStatusForRequester(userId)).thenReturn(statusCounts);

            UserDashboardResponse response = dashboardService.getUserDashboard(principal);

            assertNotNull(response);
            assertEquals(5L, response.getTotalTickets());
            assertEquals(2L, response.getOpenTickets());
            assertEquals(1L, response.getInProgressTickets());
            assertEquals(1L, response.getResolvedTickets());
            assertEquals(1L, response.getClosedTickets());

            verify(ticketRepository).countByRequesterId(userId);
            verify(ticketRepository).countByStatusForRequester(userId);
        }

        @Test
        void shouldReturnZerosForUserWithZeroTickets() {
            UUID userId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(userId, "empty@example.com", UserRole.USER);

            when(ticketRepository.countByRequesterId(userId)).thenReturn(0L);
            when(ticketRepository.countByStatusForRequester(userId)).thenReturn(List.of());

            UserDashboardResponse response = dashboardService.getUserDashboard(principal);

            assertNotNull(response);
            assertEquals(0L, response.getTotalTickets());
            assertEquals(0L, response.getOpenTickets());
            assertEquals(0L, response.getInProgressTickets());
            assertEquals(0L, response.getResolvedTickets());
            assertEquals(0L, response.getClosedTickets());
        }
    }

    @Nested
    class AgentDashboardTests {

        @Test
        void shouldReturnAgentDashboardWithCorrectCounts() {
            UUID agentId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(agentId, "agent@example.com", UserRole.SUPPORT_AGENT);

            when(ticketRepository.countByAssignedAgentId(agentId)).thenReturn(8L);
            List<Object[]> statusCounts = new ArrayList<>();
            statusCounts.add(new Object[]{TicketStatus.OPEN, 3L});
            statusCounts.add(new Object[]{TicketStatus.IN_PROGRESS, 2L});
            statusCounts.add(new Object[]{TicketStatus.RESOLVED, 2L});
            statusCounts.add(new Object[]{TicketStatus.CLOSED, 1L});
            when(ticketRepository.countByStatusForAssignedAgent(agentId)).thenReturn(statusCounts);

            AgentDashboardResponse response = dashboardService.getAgentDashboard(principal);

            assertNotNull(response);
            assertEquals(8L, response.getAssignedTickets());
            assertEquals(3L, response.getOpenTickets());
            assertEquals(2L, response.getInProgressTickets());
            assertEquals(2L, response.getResolvedTickets());
            assertEquals(1L, response.getClosedTickets());

            verify(ticketRepository).countByAssignedAgentId(agentId);
            verify(ticketRepository).countByStatusForAssignedAgent(agentId);
        }

        @Test
        void shouldReturnZerosForAgentWithZeroAssignedTickets() {
            UUID agentId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(agentId, "agent2@example.com", UserRole.SUPPORT_AGENT);

            when(ticketRepository.countByAssignedAgentId(agentId)).thenReturn(0L);
            when(ticketRepository.countByStatusForAssignedAgent(agentId)).thenReturn(List.of());

            AgentDashboardResponse response = dashboardService.getAgentDashboard(principal);

            assertNotNull(response);
            assertEquals(0L, response.getAssignedTickets());
            assertEquals(0L, response.getOpenTickets());
            assertEquals(0L, response.getInProgressTickets());
            assertEquals(0L, response.getResolvedTickets());
            assertEquals(0L, response.getClosedTickets());
        }
    }

    @Nested
    class AdminDashboardTests {

        @Test
        void shouldReturnAdminDashboardWithSystemWideCountsAndDistributions() {
            when(ticketRepository.count()).thenReturn(42L);
            when(ticketRepository.countByAssignedAgentIsNull()).thenReturn(5L);

            List<Object[]> statusCounts = new ArrayList<>();
            statusCounts.add(new Object[]{TicketStatus.OPEN, 12L});
            statusCounts.add(new Object[]{TicketStatus.IN_PROGRESS, 10L});
            statusCounts.add(new Object[]{TicketStatus.RESOLVED, 8L});
            statusCounts.add(new Object[]{TicketStatus.CLOSED, 12L});
            when(ticketRepository.countByStatusSystemWide()).thenReturn(statusCounts);

            List<Object[]> priorityCounts = new ArrayList<>();
            priorityCounts.add(new Object[]{TicketPriority.LOW, 8L});
            priorityCounts.add(new Object[]{TicketPriority.MEDIUM, 18L});
            priorityCounts.add(new Object[]{TicketPriority.HIGH, 10L});
            priorityCounts.add(new Object[]{TicketPriority.URGENT, 6L});
            when(ticketRepository.countByPrioritySystemWide()).thenReturn(priorityCounts);

            Category cat1 = new Category("Hardware", "Hardware issues");
            UUID cat1Id = UUID.randomUUID();
            cat1.setId(cat1Id);

            Category cat2 = new Category("Software", "Software issues");
            UUID cat2Id = UUID.randomUUID();
            cat2.setId(cat2Id);

            Category inactiveCat = new Category("Old Legacy", "Deprecated category");
            UUID inactiveCatId = UUID.randomUUID();
            inactiveCat.setId(inactiveCatId);
            inactiveCat.setActive(false);

            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cat1, inactiveCat, cat2));

            List<Object[]> catCounts = new ArrayList<>();
            catCounts.add(new Object[]{cat1Id, "Hardware", 20L});
            catCounts.add(new Object[]{inactiveCatId, "Old Legacy", 7L});
            catCounts.add(new Object[]{cat2Id, "Software", 15L});
            when(ticketRepository.countByCategorySystemWide()).thenReturn(catCounts);

            AdminDashboardResponse response = dashboardService.getAdminDashboard();

            assertNotNull(response);
            assertEquals(42L, response.getTotalTickets());
            assertEquals(12L, response.getOpenTickets());
            assertEquals(10L, response.getInProgressTickets());
            assertEquals(8L, response.getResolvedTickets());
            assertEquals(12L, response.getClosedTickets());
            assertEquals(5L, response.getUnassignedTickets());

            assertEquals(8L, response.getPriorityDistribution().get(TicketPriority.LOW));
            assertEquals(18L, response.getPriorityDistribution().get(TicketPriority.MEDIUM));
            assertEquals(10L, response.getPriorityDistribution().get(TicketPriority.HIGH));
            assertEquals(6L, response.getPriorityDistribution().get(TicketPriority.URGENT));

            assertEquals(3, response.getCategoryDistribution().size());
            assertEquals(20L, response.getCategoryDistribution().get(0).getTicketCount());
            assertEquals(7L, response.getCategoryDistribution().get(1).getTicketCount());
            assertEquals(15L, response.getCategoryDistribution().get(2).getTicketCount());
        }

        @Test
        void shouldHandleEmptySystemStats() {
            when(ticketRepository.count()).thenReturn(0L);
            when(ticketRepository.countByAssignedAgentIsNull()).thenReturn(0L);
            when(ticketRepository.countByStatusSystemWide()).thenReturn(List.of());
            when(ticketRepository.countByPrioritySystemWide()).thenReturn(List.of());
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
            when(ticketRepository.countByCategorySystemWide()).thenReturn(List.of());

            AdminDashboardResponse response = dashboardService.getAdminDashboard();

            assertNotNull(response);
            assertEquals(0L, response.getTotalTickets());
            assertEquals(0L, response.getUnassignedTickets());
            assertEquals(0L, response.getPriorityDistribution().get(TicketPriority.URGENT));
            assertEquals(0, response.getCategoryDistribution().size());
        }
    }
}
