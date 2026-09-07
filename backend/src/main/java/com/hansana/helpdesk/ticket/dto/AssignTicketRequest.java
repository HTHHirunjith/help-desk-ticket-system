package com.hansana.helpdesk.ticket.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTicketRequest(
        @NotNull(message = "Agent ID is required")
        UUID agentId
) {
}

