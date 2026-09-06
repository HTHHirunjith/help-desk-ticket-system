package com.hansana.helpdesk.ticket.repository;

import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNumber(Long ticketNumber);

    boolean existsByTicketNumber(Long ticketNumber);

    Page<Ticket> findByRequesterId(UUID requesterId, Pageable pageable);

    Page<Ticket> findByAssignedAgentId(UUID assignedAgentId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);

    Page<Ticket> findByCategoryId(UUID categoryId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.requester.id = :requesterId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId)")
    Page<Ticket> findByRequesterWithFilters(
            @org.springframework.data.repository.query.Param("requesterId") UUID requesterId,
            @org.springframework.data.repository.query.Param("status") TicketStatus status,
            @org.springframework.data.repository.query.Param("priority") TicketPriority priority,
            @org.springframework.data.repository.query.Param("categoryId") UUID categoryId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.assignedAgent.id = :agentId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId)")
    Page<Ticket> findByAssignedAgentWithFilters(
            @org.springframework.data.repository.query.Param("agentId") UUID agentId,
            @org.springframework.data.repository.query.Param("status") TicketStatus status,
            @org.springframework.data.repository.query.Param("priority") TicketPriority priority,
            @org.springframework.data.repository.query.Param("categoryId") UUID categoryId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE " +
           "(:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId)")
    Page<Ticket> findAllWithFilters(
            @org.springframework.data.repository.query.Param("status") TicketStatus status,
            @org.springframework.data.repository.query.Param("priority") TicketPriority priority,
            @org.springframework.data.repository.query.Param("categoryId") UUID categoryId,
            Pageable pageable);
}
