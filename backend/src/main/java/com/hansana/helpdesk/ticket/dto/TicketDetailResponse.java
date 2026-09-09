package com.hansana.helpdesk.ticket.dto;

import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketDetailResponse(
        UUID id,
        Long ticketNumber,
        String title,
        String description,
        CategoryResponse category,
        TicketPriority priority,
        TicketStatus status,
        TicketSummaryResponse.UserRef requester,
        TicketSummaryResponse.UserRef assignedAgent,
        Instant resolutionConfirmedAt,
        TicketSummaryResponse.UserRef resolutionConfirmedBy,
        Instant reopenedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketDetailResponse from(Ticket ticket) {
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                CategoryResponse.from(ticket.getCategory()),
                ticket.getPriority(),
                ticket.getStatus(),
                TicketSummaryResponse.UserRef.from(ticket.getRequester()),
                TicketSummaryResponse.UserRef.from(ticket.getAssignedAgent()),
                ticket.getResolutionConfirmedAt(),
                TicketSummaryResponse.UserRef.from(ticket.getResolutionConfirmedBy()),
                ticket.getReopenedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}


