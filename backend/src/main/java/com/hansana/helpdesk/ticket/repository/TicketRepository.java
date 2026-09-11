package com.hansana.helpdesk.ticket.repository;

import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "SELECT t.* FROM tickets t " +
           "WHERE t.requester_id = :requesterId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category_id = CAST(:categoryId AS uuid)) " +
           "AND (:search IS NULL OR (" +
           "t.title ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "t.description ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "CAST(t.ticket_number AS TEXT) LIKE CONCAT('%', :search, '%') ESCAPE '!')) " +
           "ORDER BY t.updated_at DESC",
           countQuery = "SELECT COUNT(t.id) FROM tickets t " +
           "WHERE t.requester_id = :requesterId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category_id = CAST(:categoryId AS uuid)) " +
           "AND (:search IS NULL OR (" +
           "t.title ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "t.description ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "CAST(t.ticket_number AS TEXT) LIKE CONCAT('%', :search, '%') ESCAPE '!'))",
           nativeQuery = true)
    Page<Ticket> findByRequesterWithFilters(
            @Param("requesterId") UUID requesterId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = "SELECT t.* FROM tickets t " +
           "WHERE t.assigned_agent_id = :agentId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category_id = CAST(:categoryId AS uuid)) " +
           "AND (:search IS NULL OR (" +
           "t.title ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "t.description ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "CAST(t.ticket_number AS TEXT) LIKE CONCAT('%', :search, '%') ESCAPE '!')) " +
           "ORDER BY t.updated_at DESC",
           countQuery = "SELECT COUNT(t.id) FROM tickets t " +
           "WHERE t.assigned_agent_id = :agentId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category_id = CAST(:categoryId AS uuid)) " +
           "AND (:search IS NULL OR (" +
           "t.title ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "t.description ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "CAST(t.ticket_number AS TEXT) LIKE CONCAT('%', :search, '%') ESCAPE '!'))",
           nativeQuery = true)
    Page<Ticket> findByAssignedAgentWithFilters(
            @Param("agentId") UUID agentId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = "SELECT t.* FROM tickets t " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category_id = CAST(:categoryId AS uuid)) " +
           "AND (:search IS NULL OR (" +
           "t.title ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "t.description ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "CAST(t.ticket_number AS TEXT) LIKE CONCAT('%', :search, '%') ESCAPE '!')) " +
           "ORDER BY t.updated_at DESC",
           countQuery = "SELECT COUNT(t.id) FROM tickets t " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.category_id = CAST(:categoryId AS uuid)) " +
           "AND (:search IS NULL OR (" +
           "t.title ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "t.description ILIKE CONCAT('%', :search, '%') ESCAPE '!' OR " +
           "CAST(t.ticket_number AS TEXT) LIKE CONCAT('%', :search, '%') ESCAPE '!'))",
           nativeQuery = true)
    Page<Ticket> findAllWithFilters(
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable);
}
