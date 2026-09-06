package com.hansana.helpdesk.ticket.dto;

import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        Long ticketNumber,
        String title,
        CategoryResponse category,
        TicketPriority priority,
        TicketStatus status,
        UserRef requester,
        UserRef assignedAgent,
        Instant createdAt,
        Instant updatedAt
) {
    public record UserRef(UUID id, String name) {
        public static UserRef from(User user) {
            if (user == null) {
                return null;
            }
            return new UserRef(user.getId(), user.getFirstName() + " " + user.getLastName());
        }
    }

    public static TicketSummaryResponse from(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                CategoryResponse.from(ticket.getCategory()),
                ticket.getPriority(),
                ticket.getStatus(),
                UserRef.from(ticket.getRequester()),
                UserRef.from(ticket.getAssignedAgent()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
