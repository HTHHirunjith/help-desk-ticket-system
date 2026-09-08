package com.hansana.helpdesk.audit.dto;

import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        AuditAction action,
        ActorRef actor,
        String details,
        Instant createdAt
) {
    public record ActorRef(UUID id, String name, String email, UserRole role) {
        public static ActorRef from(User user) {
            if (user == null) {
                return null;
            }
            return new ActorRef(
                    user.getId(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    user.getRole()
            );
        }
    }

    public static AuditResponse from(TicketAudit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getAction(),
                ActorRef.from(audit.getActor()),
                audit.getDetails(),
                audit.getCreatedAt()
        );
    }
}
