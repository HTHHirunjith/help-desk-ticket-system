package com.hansana.helpdesk.dashboard.dto;

import com.hansana.helpdesk.ticket.entity.TicketPriority;

import java.util.List;
import java.util.Map;

public class AdminDashboardResponse {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long unassignedTickets;
    private Map<TicketPriority, Long> priorityDistribution;
    private List<CategoryDistributionItem> categoryDistribution;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(long totalTickets,
                                  long openTickets,
                                  long inProgressTickets,
                                  long resolvedTickets,
                                  long closedTickets,
                                  long unassignedTickets,
                                  Map<TicketPriority, Long> priorityDistribution,
                                  List<CategoryDistributionItem> categoryDistribution) {
        this.totalTickets = totalTickets;
        this.openTickets = openTickets;
        this.inProgressTickets = inProgressTickets;
        this.resolvedTickets = resolvedTickets;
        this.closedTickets = closedTickets;
        this.unassignedTickets = unassignedTickets;
        this.priorityDistribution = priorityDistribution;
        this.categoryDistribution = categoryDistribution;
    }

    public long getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(long totalTickets) {
        this.totalTickets = totalTickets;
    }

    public long getOpenTickets() {
        return openTickets;
    }

    public void setOpenTickets(long openTickets) {
        this.openTickets = openTickets;
    }

    public long getInProgressTickets() {
        return inProgressTickets;
    }

    public void setInProgressTickets(long inProgressTickets) {
        this.inProgressTickets = inProgressTickets;
    }

    public long getResolvedTickets() {
        return resolvedTickets;
    }

    public void setResolvedTickets(long resolvedTickets) {
        this.resolvedTickets = resolvedTickets;
    }

    public long getClosedTickets() {
        return closedTickets;
    }

    public void setClosedTickets(long closedTickets) {
        this.closedTickets = closedTickets;
    }

    public long getUnassignedTickets() {
        return unassignedTickets;
    }

    public void setUnassignedTickets(long unassignedTickets) {
        this.unassignedTickets = unassignedTickets;
    }

    public Map<TicketPriority, Long> getPriorityDistribution() {
        return priorityDistribution;
    }

    public void setPriorityDistribution(Map<TicketPriority, Long> priorityDistribution) {
        this.priorityDistribution = priorityDistribution;
    }

    public List<CategoryDistributionItem> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<CategoryDistributionItem> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }
}
