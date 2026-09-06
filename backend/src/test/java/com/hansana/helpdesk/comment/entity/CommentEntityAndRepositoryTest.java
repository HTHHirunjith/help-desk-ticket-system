package com.hansana.helpdesk.comment.entity;

import com.hansana.helpdesk.comment.repository.CommentRepository;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommentEntityAndRepositoryTest {

    @Test
    void commentEntityMapsToCommentsTable() throws NoSuchFieldException {
        Table table = Comment.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("comments", table.name());

        assertEquals("TEXT", columnDefinition(Comment.class.getDeclaredField("body")));
        assertEquals("created_at", columnName(Comment.class.getDeclaredField("createdAt")));
        assertEquals("updated_at", columnName(Comment.class.getDeclaredField("updatedAt")));
    }

    @Test
    void commentRelationshipsAreConfigured() throws NoSuchFieldException {
        Field ticketField = Comment.class.getDeclaredField("ticket");
        ManyToOne ticketRel = ticketField.getAnnotation(ManyToOne.class);
        assertNotNull(ticketRel);
        assertEquals(FetchType.LAZY, ticketRel.fetch());
        assertFalse(ticketRel.optional());
        JoinColumn ticketJoin = ticketField.getAnnotation(JoinColumn.class);
        assertEquals("ticket_id", ticketJoin.name());

        Field authorField = Comment.class.getDeclaredField("author");
        ManyToOne authorRel = authorField.getAnnotation(ManyToOne.class);
        assertNotNull(authorRel);
        assertEquals(FetchType.LAZY, authorRel.fetch());
        assertFalse(authorRel.optional());
        JoinColumn authorJoin = authorField.getAnnotation(JoinColumn.class);
        assertEquals("author_id", authorJoin.name());
    }

    @Test
    void commentLifecycleAndFields() {
        Ticket ticket = new Ticket();
        User author = new User();
        Comment comment = new Comment(ticket, author, "Investigating this now.");

        assertEquals(ticket, comment.getTicket());
        assertEquals(author, comment.getAuthor());
        assertEquals("Investigating this now.", comment.getBody());
        assertNull(comment.getCreatedAt());
        assertNull(comment.getUpdatedAt());

        comment.onCreate();
        assertNotNull(comment.getCreatedAt());
        assertNotNull(comment.getUpdatedAt());
    }

    @Test
    void commentRepositoryDeclaresFindByTicketId() throws NoSuchMethodException {
        Method method = CommentRepository.class.getMethod("findByTicketIdOrderByCreatedAtAsc", UUID.class);
        assertEquals(List.class, method.getReturnType());
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
