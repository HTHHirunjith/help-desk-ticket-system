package com.hansana.helpdesk.ticket.entity;

import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import com.hansana.helpdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketEntityAndRepositoryTest {

    @Test
    void ticketEntityMapsToTicketsTable() throws NoSuchFieldException {
        Table table = Ticket.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("tickets", table.name());

        Field ticketNumberField = Ticket.class.getDeclaredField("ticketNumber");
        Column ticketNumberCol = ticketNumberField.getAnnotation(Column.class);
        assertNotNull(ticketNumberCol);
        assertEquals("ticket_number", ticketNumberCol.name());
        assertFalse(ticketNumberCol.insertable());
        assertFalse(ticketNumberCol.updatable());

        assertEquals("title", columnName(Ticket.class.getDeclaredField("title")));
        assertEquals("TEXT", columnDefinition(Ticket.class.getDeclaredField("description")));
        assertEquals("created_at", columnName(Ticket.class.getDeclaredField("createdAt")));
        assertEquals("updated_at", columnName(Ticket.class.getDeclaredField("updatedAt")));
        assertEquals("resolution_confirmed_at", columnName(Ticket.class.getDeclaredField("resolutionConfirmedAt")));
    }

    @Test
    void ticketRelationshipsAreConfiguredCorrectly() throws NoSuchFieldException {
        Field categoryField = Ticket.class.getDeclaredField("category");
        ManyToOne categoryRel = categoryField.getAnnotation(ManyToOne.class);
        assertNotNull(categoryRel);
        assertEquals(FetchType.LAZY, categoryRel.fetch());
        assertFalse(categoryRel.optional());
        JoinColumn categoryJoin = categoryField.getAnnotation(JoinColumn.class);
        assertEquals("category_id", categoryJoin.name());

        Field requesterField = Ticket.class.getDeclaredField("requester");
        ManyToOne requesterRel = requesterField.getAnnotation(ManyToOne.class);
        assertNotNull(requesterRel);
        assertEquals(FetchType.LAZY, requesterRel.fetch());
        assertFalse(requesterRel.optional());
        JoinColumn requesterJoin = requesterField.getAnnotation(JoinColumn.class);
        assertEquals("requester_id", requesterJoin.name());

        Field agentField = Ticket.class.getDeclaredField("assignedAgent");
        ManyToOne agentRel = agentField.getAnnotation(ManyToOne.class);
        assertNotNull(agentRel);
        assertEquals(FetchType.LAZY, agentRel.fetch());
        assertTrue(agentRel.optional());
        JoinColumn agentJoin = agentField.getAnnotation(JoinColumn.class);
        assertEquals("assigned_agent_id", agentJoin.name());

        Field confirmerField = Ticket.class.getDeclaredField("resolutionConfirmedBy");
        ManyToOne confirmerRel = confirmerField.getAnnotation(ManyToOne.class);
        assertNotNull(confirmerRel);
        assertEquals(FetchType.LAZY, confirmerRel.fetch());
        JoinColumn confirmerJoin = confirmerField.getAnnotation(JoinColumn.class);
        assertEquals("resolution_confirmed_by", confirmerJoin.name());
    }

    @Test
    void ticketEnumsAreConfiguredWithStringType() throws NoSuchFieldException {
        Field priorityField = Ticket.class.getDeclaredField("priority");
        Enumerated priorityEnum = priorityField.getAnnotation(Enumerated.class);
        assertNotNull(priorityEnum);
        assertEquals(EnumType.STRING, priorityEnum.value());

        Field statusField = Ticket.class.getDeclaredField("status");
        Enumerated statusEnum = statusField.getAnnotation(Enumerated.class);
        assertNotNull(statusEnum);
        assertEquals(EnumType.STRING, statusEnum.value());
    }

    @Test
    void ticketControlledValuesMatchSpecification() {
        assertEquals(4, TicketPriority.values().length);
        assertNotNull(TicketPriority.valueOf("LOW"));
        assertNotNull(TicketPriority.valueOf("MEDIUM"));
        assertNotNull(TicketPriority.valueOf("HIGH"));
        assertNotNull(TicketPriority.valueOf("URGENT"));

        assertEquals(4, TicketStatus.values().length);
        assertNotNull(TicketStatus.valueOf("OPEN"));
        assertNotNull(TicketStatus.valueOf("IN_PROGRESS"));
        assertNotNull(TicketStatus.valueOf("RESOLVED"));
        assertNotNull(TicketStatus.valueOf("CLOSED"));
    }

    @Test
    void ticketDefaultsAndPrePersist() {
        Category category = new Category("GENERAL", "General");
        User requester = new User();
        requester.setId(UUID.randomUUID());

        Ticket ticket = new Ticket("Login issue", "Cannot login", category, null, requester);
        assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getAssignedAgent());
        assertNull(ticket.getResolutionConfirmedAt());
        assertNull(ticket.getResolutionConfirmedBy());

        ticket.onCreate();
        assertNotNull(ticket.getCreatedAt());
        assertNotNull(ticket.getUpdatedAt());
    }

    @Test
    void ticketResolutionConfirmationPairFields() {
        Ticket ticket = new Ticket();
        Instant now = Instant.now();
        User confirmer = new User();
        confirmer.setId(UUID.randomUUID());

        ticket.setResolutionConfirmedAt(now);
        ticket.setResolutionConfirmedBy(confirmer);

        assertEquals(now, ticket.getResolutionConfirmedAt());
        assertEquals(confirmer, ticket.getResolutionConfirmedBy());
    }

    @Test
    void ticketRepositoryDeclaresFindByTicketNumber() throws NoSuchMethodException {
        Method method = TicketRepository.class.getMethod("findByTicketNumber", Long.class);
        assertEquals(Optional.class, method.getReturnType());
    }

    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        return column.name().isEmpty() ? field.getName() : column.name();
    }

    private static String columnDefinition(Field field) {
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        return column.columnDefinition();
    }
}
