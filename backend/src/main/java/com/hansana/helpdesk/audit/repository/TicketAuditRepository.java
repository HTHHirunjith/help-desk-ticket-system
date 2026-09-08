package com.hansana.helpdesk.audit.repository;

import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketAuditRepository extends JpaRepository<TicketAudit, UUID> {

    List<TicketAudit> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    @Query("SELECT a FROM TicketAudit a LEFT JOIN FETCH a.actor WHERE a.ticket.id = :ticketId ORDER BY a.createdAt DESC")
    List<TicketAudit> findByTicketIdOrderByCreatedAtDesc(@Param("ticketId") UUID ticketId);

    List<TicketAudit> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    List<TicketAudit> findByActionOrderByCreatedAtDesc(AuditAction action);
}

