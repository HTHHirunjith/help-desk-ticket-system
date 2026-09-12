package com.hansana.helpdesk.dashboard.service;

import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.dashboard.dto.AdminDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.AgentDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.CategoryDistributionItem;
import com.hansana.helpdesk.dashboard.dto.UserDashboardResponse;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;

    public DashboardService(TicketRepository ticketRepository, CategoryRepository categoryRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
    }

    public UserDashboardResponse getUserDashboard(UserPrincipal principal) {
        UUID userId = principal.getId();
        long totalTickets = ticketRepository.countByRequesterId(userId);

        Map<TicketStatus, Long> statusCounts = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            statusCounts.put(status, 0L);
        }

        List<Object[]> rows = ticketRepository.countByStatusForRequester(userId);
        for (Object[] row : rows) {
            TicketStatus status = (TicketStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            statusCounts.put(status, count);
        }

        return new UserDashboardResponse(
                totalTickets,
                statusCounts.get(TicketStatus.OPEN),
                statusCounts.get(TicketStatus.IN_PROGRESS),
                statusCounts.get(TicketStatus.RESOLVED),
                statusCounts.get(TicketStatus.CLOSED)
        );
    }

    public AgentDashboardResponse getAgentDashboard(UserPrincipal principal) {
        UUID agentId = principal.getId();
        long assignedTickets = ticketRepository.countByAssignedAgentId(agentId);

        Map<TicketStatus, Long> statusCounts = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            statusCounts.put(status, 0L);
        }

        List<Object[]> rows = ticketRepository.countByStatusForAssignedAgent(agentId);
        for (Object[] row : rows) {
            TicketStatus status = (TicketStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            statusCounts.put(status, count);
        }

        return new AgentDashboardResponse(
                assignedTickets,
                statusCounts.get(TicketStatus.OPEN),
                statusCounts.get(TicketStatus.IN_PROGRESS),
                statusCounts.get(TicketStatus.RESOLVED),
                statusCounts.get(TicketStatus.CLOSED)
        );
    }

    public AdminDashboardResponse getAdminDashboard() {
        long totalTickets = ticketRepository.count();
        long unassignedTickets = ticketRepository.countByAssignedAgentIsNull();

        Map<TicketStatus, Long> statusCounts = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            statusCounts.put(status, 0L);
        }

        List<Object[]> statusRows = ticketRepository.countByStatusSystemWide();
        for (Object[] row : statusRows) {
            TicketStatus status = (TicketStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            statusCounts.put(status, count);
        }

        Map<TicketPriority, Long> priorityDistribution = new LinkedHashMap<>();
        for (TicketPriority priority : TicketPriority.values()) {
            priorityDistribution.put(priority, 0L);
        }

        List<Object[]> priorityRows = ticketRepository.countByPrioritySystemWide();
        for (Object[] row : priorityRows) {
            TicketPriority priority = (TicketPriority) row[0];
            Long count = ((Number) row[1]).longValue();
            priorityDistribution.put(priority, count);
        }

        // Fetch all categories (active & inactive) ordered by name to build complete category distribution
        List<Category> allCategories = categoryRepository.findAllByOrderByNameAsc();
        Map<UUID, CategoryDistributionItem> categoryMap = new LinkedHashMap<>();
        for (Category cat : allCategories) {
            categoryMap.put(cat.getId(), new CategoryDistributionItem(cat.getId(), cat.getName(), 0L));
        }

        List<Object[]> categoryRows = ticketRepository.countByCategorySystemWide();
        for (Object[] row : categoryRows) {
            UUID catId = (UUID) row[0];
            String catName = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            if (categoryMap.containsKey(catId)) {
                categoryMap.get(catId).setTicketCount(count);
            } else {
                categoryMap.put(catId, new CategoryDistributionItem(catId, catName, count));
            }
        }

        List<CategoryDistributionItem> categoryDistribution = new ArrayList<>(categoryMap.values());

        return new AdminDashboardResponse(
                totalTickets,
                statusCounts.get(TicketStatus.OPEN),
                statusCounts.get(TicketStatus.IN_PROGRESS),
                statusCounts.get(TicketStatus.RESOLVED),
                statusCounts.get(TicketStatus.CLOSED),
                unassignedTickets,
                priorityDistribution,
                categoryDistribution
        );
    }
}
