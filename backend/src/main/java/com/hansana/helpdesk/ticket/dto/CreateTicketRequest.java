package com.hansana.helpdesk.ticket.dto;

import com.hansana.helpdesk.ticket.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotNull(message = "Priority is required")
        TicketPriority priority
) {
}
