package com.hansana.helpdesk.audit.entity;

import com.hansana.helpdesk.audit.repository.TicketAuditRepository;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketAuditEntityAndRepositoryTest {

    @Test
    void auditEntityMapsToTicketAuditsTable() throws NoSuchFieldException {
        Table table = TicketAudit.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("ticket_audits", table.name());

        Field actionField = TicketAudit.class.getDeclaredField("action");
        Enumerated enumerated = actionField.getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());

        Field detailsField = TicketAudit.class.getDeclaredField("details");
        JdbcTypeCode jdbcTypeCode = detailsField.getAnnotation(JdbcTypeCode.class);
        assertNotNull(jdbcTypeCode);
        assertEquals(SqlTypes.JSON, jdbcTypeCode.value());
        Column detailsCol = detailsField.getAnnotation(Column.class);
        assertEquals("jsonb", detailsCol.columnDefinition());

        assertEquals("created_at", columnName(TicketAudit.class.getDeclaredField("createdAt")));
    }

    @Test
    void auditActionsMatchApprovedList() {
        assertEquals(11, AuditAction.values().length);
        assertNotNull(AuditAction.valueOf("TICKET_CREATED"));
        assertNotNull(AuditAction.valueOf("TICKET_ASSIGNED"));
        assertNotNull(AuditAction.valueOf("TICKET_REASSIGNED"));
        assertNotNull(AuditAction.valueOf("TICKET_UNASSIGNED"));
        assertNotNull(AuditAction.valueOf("STATUS_CHANGED"));
        assertNotNull(AuditAction.valueOf("PRIORITY_CHANGED"));
        assertNotNull(AuditAction.valueOf("TICKET_REOPENED"));
        assertNotNull(AuditAction.valueOf("RESOLUTION_CONFIRMED"));
        assertNotNull(AuditAction.valueOf("TICKET_CLOSED"));
        assertNotNull(AuditAction.valueOf("COMMENT_ADDED"));
        assertNotNull(AuditAction.valueOf("CATEGORY_CHANGED"));
    }

    @Test
    void auditRelationshipsAndNullableActor() throws NoSuchFieldException {
        Field ticketField = TicketAudit.class.getDeclaredField("ticket");
        ManyToOne ticketRel = ticketField.getAnnotation(ManyToOne.class);
        assertNotNull(ticketRel);
        assertEquals(FetchType.LAZY, ticketRel.fetch());
        assertFalse(ticketRel.optional());
        JoinColumn ticketJoin = ticketField.getAnnotation(JoinColumn.class);
        assertEquals("ticket_id", ticketJoin.name());

        Field actorField = TicketAudit.class.getDeclaredField("actor");
        ManyToOne actorRel = actorField.getAnnotation(ManyToOne.class);
        assertNotNull(actorRel);
        assertEquals(FetchType.LAZY, actorRel.fetch());
        assertTrue(actorRel.optional()); // actor is nullable for system events
        JoinColumn actorJoin = actorField.getAnnotation(JoinColumn.class);
        assertEquals("actor_id", actorJoin.name());
    }

    @Test
    void auditLifecycleAndFields() {
        Ticket ticket = new Ticket();
        User actor = new User();
        String jsonDetails = "{\"oldStatus\":\"OPEN\",\"newStatus\":\"IN_PROGRESS\"}";
        TicketAudit audit = new TicketAudit(ticket, actor, AuditAction.STATUS_CHANGED, jsonDetails);

        assertEquals(ticket, audit.getTicket());
        assertEquals(actor, audit.getActor());
        assertEquals(AuditAction.STATUS_CHANGED, audit.getAction());
        assertEquals(jsonDetails, audit.getDetails());
        assertNull(audit.getCreatedAt());

        audit.onCreate();
        assertNotNull(audit.getCreatedAt());
    }

    @Test
    void auditAllowsNullActorForSystemEvents() {
        Ticket ticket = new Ticket();
        TicketAudit systemAudit = new TicketAudit(ticket, null, AuditAction.TICKET_CREATED, null);
        assertNull(systemAudit.getActor());
    }

    @Test
    void auditRepositoryDeclaresExpectedQueries() throws NoSuchMethodException {
        Method byTicket = TicketAuditRepository.class.getMethod("findByTicketIdOrderByCreatedAtAsc", UUID.class);
        assertEquals(List.class, byTicket.getReturnType());

        Method byActor = TicketAuditRepository.class.getMethod("findByActorIdOrderByCreatedAtDesc", UUID.class);
        assertEquals(List.class, byActor.getReturnType());

        Method byAction = TicketAuditRepository.class.getMethod("findByActionOrderByCreatedAtDesc", AuditAction.class);
        assertEquals(List.class, byAction.getReturnType());
    }

    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        return column.name().isEmpty() ? field.getName() : column.name();
    }
}
