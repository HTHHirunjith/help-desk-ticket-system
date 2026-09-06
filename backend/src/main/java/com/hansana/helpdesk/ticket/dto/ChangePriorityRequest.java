package com.hansana.helpdesk.ticket.dto;

import com.hansana.helpdesk.ticket.entity.TicketPriority;
import jakarta.validation.constraints.NotNull;

public record ChangePriorityRequest(
        @NotNull(message = "Priority is required")
        TicketPriority priority
) {
}
