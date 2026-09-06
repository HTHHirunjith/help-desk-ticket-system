package com.hansana.helpdesk.audit.repository;

import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketAuditRepository extends JpaRepository<TicketAudit, UUID> {

    List<TicketAudit> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<TicketAudit> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    List<TicketAudit> findByActionOrderByCreatedAtDesc(AuditAction action);
}
