package com.hansana.helpdesk.ticket.dto;

import java.util.UUID;

public record UpdateTicketRequest(
        String title,
        String description,
        UUID categoryId
) {
    public boolean hasAnyField() {
        return (title != null && !title.isBlank()) ||
               (description != null && !description.isBlank()) ||
               categoryId != null;
    }
}

