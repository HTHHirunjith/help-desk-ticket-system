package com.hansana.helpdesk.ticket.service;

import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import com.hansana.helpdesk.audit.repository.TicketAuditRepository;
import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.common.dto.PagedResponse;
import com.hansana.helpdesk.common.exception.InvalidTicketStateException;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.common.exception.TicketNotEditableException;
import com.hansana.helpdesk.ticket.dto.AssignTicketRequest;
import com.hansana.helpdesk.ticket.dto.ChangePriorityRequest;
import com.hansana.helpdesk.ticket.dto.CreateTicketRequest;
import com.hansana.helpdesk.ticket.dto.TicketDetailResponse;
import com.hansana.helpdesk.ticket.dto.TicketSummaryResponse;
import com.hansana.helpdesk.ticket.dto.UpdateTicketRequest;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TicketAuditRepository ticketAuditRepository;

    public TicketService(TicketRepository ticketRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository,
                         TicketAuditRepository ticketAuditRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.ticketAuditRepository = ticketAuditRepository;
    }

    public TicketDetailResponse createTicket(CreateTicketRequest request, UserPrincipal principal) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Category is not active");
        }

        User requester = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        Ticket ticket = new Ticket(
                request.title(),
                request.description(),
                category,
                request.priority(),
                requester
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketDetailResponse.from(savedTicket);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TicketSummaryResponse> listTickets(
            UserPrincipal principal,
            String search,
            TicketStatus status,
            TicketPriority priority,
            UUID categoryId,
            Pageable pageable) {

        String sanitizedSearch = sanitizeSearch(search);
        String statusStr = (status != null) ? status.name() : null;
        String priorityStr = (priority != null) ? priority.name() : null;
        Pageable repoPageable = (pageable != null)
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : PageRequest.of(0, 20);
        Page<Ticket> page;
        UserRole role = principal.getRole();

        if (role == UserRole.USER) {
            page = ticketRepository.findByRequesterWithFilters(principal.getId(), statusStr, priorityStr, categoryId, sanitizedSearch, repoPageable);
        } else if (role == UserRole.SUPPORT_AGENT) {
            page = ticketRepository.findByAssignedAgentWithFilters(principal.getId(), statusStr, priorityStr, categoryId, sanitizedSearch, repoPageable);
        } else {
            page = ticketRepository.findAllWithFilters(statusStr, priorityStr, categoryId, sanitizedSearch, repoPageable);
        }

        return PagedResponse.from(page.map(TicketSummaryResponse::from));
    }

    private String sanitizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()) {
            return null;
        }
        return escapeLike(trimmed);
    }

    private String escapeLike(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicket(UUID ticketId, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        UserRole role = principal.getRole();
        if (role == UserRole.USER) {
            if (!ticket.getRequester().getId().equals(principal.getId())) {
                throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
            }
        } else if (role == UserRole.SUPPORT_AGENT) {
            if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(principal.getId())) {
                throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
            }
        }

        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse updateOpenTicket(UUID ticketId, UpdateTicketRequest request, UserPrincipal principal) {
        // 1. Load ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        // 2. Verify the authenticated user is the requester
        // 3. If not the requester, throw ResourceNotFoundException
        if (!ticket.getRequester().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        // 4. Only after the ownership check, verify ticket.status == OPEN
        // 5. If not OPEN, throw TicketNotEditableException -> 409
        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new TicketNotEditableException("Ticket cannot be edited in its current status: " + ticket.getStatus());
        }

        // 6. Validate that at least one editable field was supplied
        if (request == null || !request.hasAnyField()) {
            throw new IllegalArgumentException("At least one editable field must be provided");
        }

        // 7. Validate supplied category
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            if (!category.isActive()) {
                throw new IllegalArgumentException("Category is not active");
            }
            ticket.setCategory(category);
        }

        // 8. Apply partial update
        if (request.title() != null && !request.title().isBlank()) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(request.description());
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketDetailResponse.from(savedTicket);
    }

    public TicketDetailResponse changePriority(UUID ticketId, ChangePriorityRequest request, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        UserRole role = principal.getRole();
        if (role == UserRole.SUPPORT_AGENT) {
            if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(principal.getId())) {
                throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
            }
        }

        if (ticket.getPriority() == request.priority()) {
            return TicketDetailResponse.from(ticket);
        }

        ticket.setPriority(request.priority());
        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketDetailResponse.from(savedTicket);
    }

    public TicketDetailResponse assignTicket(UUID ticketId, AssignTicketRequest request, UserPrincipal principal) {
        if (principal.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can assign tickets");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        User targetAgent = userRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.agentId()));

        if (!targetAgent.isActive()) {
            throw new IllegalArgumentException("Cannot assign ticket to inactive user");
        }

        if (targetAgent.getRole() != UserRole.SUPPORT_AGENT) {
            throw new IllegalArgumentException("Target user is not a SUPPORT_AGENT");
        }

        User previousAgent = ticket.getAssignedAgent();

        if (previousAgent != null && previousAgent.getId().equals(targetAgent.getId())) {
            throw new InvalidTicketStateException("Ticket is already assigned to this agent");
        }

        AuditAction action;
        String details;
        if (previousAgent == null) {
            action = AuditAction.TICKET_ASSIGNED;
            details = String.format("{\"newAgentId\":\"%s\"}", targetAgent.getId());
        } else {
            action = AuditAction.TICKET_REASSIGNED;
            details = String.format("{\"previousAgentId\":\"%s\",\"newAgentId\":\"%s\"}", previousAgent.getId(), targetAgent.getId());
        }

        ticket.setAssignedAgent(targetAgent);
        Ticket savedTicket = ticketRepository.save(ticket);

        TicketAudit audit = new TicketAudit(savedTicket, actor, action, details);
        ticketAuditRepository.save(audit);

        return TicketDetailResponse.from(savedTicket);
    }

    public void unassignTicket(UUID ticketId, UserPrincipal principal) {
        if (principal.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can unassign tickets");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        User previousAgent = ticket.getAssignedAgent();
        if (previousAgent == null) {
            throw new InvalidTicketStateException("Ticket is already unassigned");
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        ticket.setAssignedAgent(null);
        Ticket savedTicket = ticketRepository.save(ticket);

        String details = String.format("{\"previousAgentId\":\"%s\"}", previousAgent.getId());
        TicketAudit audit = new TicketAudit(savedTicket, actor, AuditAction.TICKET_UNASSIGNED, details);
        ticketAuditRepository.save(audit);
    }

    public TicketDetailResponse startWork(UUID ticketId, UserPrincipal principal) {
        if (principal.getRole() != UserRole.SUPPORT_AGENT) {
            throw new AccessDeniedException("Only SUPPORT_AGENT can start work on tickets");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new InvalidTicketStateException("Cannot start work on ticket in status: " + ticket.getStatus());
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        Ticket savedTicket = ticketRepository.save(ticket);

        String details = "{\"from\":\"OPEN\",\"to\":\"IN_PROGRESS\"}";
        TicketAudit audit = new TicketAudit(savedTicket, actor, AuditAction.STATUS_CHANGED, details);
        ticketAuditRepository.save(audit);

        return TicketDetailResponse.from(savedTicket);
    }

    public TicketDetailResponse resolveTicket(UUID ticketId, UserPrincipal principal) {
        if (principal.getRole() != UserRole.SUPPORT_AGENT) {
            throw new AccessDeniedException("Only SUPPORT_AGENT can resolve tickets");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Cannot resolve ticket in status: " + ticket.getStatus());
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        ticket.setStatus(TicketStatus.RESOLVED);
        Ticket savedTicket = ticketRepository.save(ticket);

        String details = "{\"from\":\"IN_PROGRESS\",\"to\":\"RESOLVED\"}";
        TicketAudit audit = new TicketAudit(savedTicket, actor, AuditAction.STATUS_CHANGED, details);
        ticketAuditRepository.save(audit);

        return TicketDetailResponse.from(savedTicket);
    }

    public TicketDetailResponse confirmResolution(UUID ticketId, UserPrincipal principal) {
        if (principal.getRole() != UserRole.USER) {
            throw new AccessDeniedException("Only USER can confirm resolution");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (!ticket.getRequester().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidTicketStateException("Cannot confirm resolution on ticket in status: " + ticket.getStatus());
        }

        if (ticket.getResolutionConfirmedAt() != null) {
            throw new InvalidTicketStateException("Resolution has already been confirmed");
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        ticket.setResolutionConfirmedAt(Instant.now());
        ticket.setResolutionConfirmedBy(actor);
        Ticket savedTicket = ticketRepository.save(ticket);

        TicketAudit audit = new TicketAudit(savedTicket, actor, AuditAction.RESOLUTION_CONFIRMED, null);
        ticketAuditRepository.save(audit);

        return TicketDetailResponse.from(savedTicket);
    }

    public TicketDetailResponse rejectResolution(UUID ticketId, UserPrincipal principal) {
        if (principal.getRole() != UserRole.USER) {
            throw new AccessDeniedException("Only USER can reject resolution");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (!ticket.getRequester().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidTicketStateException("Cannot reject resolution on ticket in status: " + ticket.getStatus());
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setResolutionConfirmedAt(null);
        ticket.setResolutionConfirmedBy(null);
        ticket.setReopenedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        Ticket savedTicket = ticketRepository.save(ticket);

        TicketAudit audit = new TicketAudit(savedTicket, actor, AuditAction.TICKET_REOPENED, null);
        ticketAuditRepository.save(audit);

        return TicketDetailResponse.from(savedTicket);
    }

    public TicketDetailResponse closeTicket(UUID ticketId, UserPrincipal principal) {
        if (principal.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can close tickets");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidTicketStateException("Cannot close ticket in status: " + ticket.getStatus());
        }

        if (ticket.getResolutionConfirmedAt() == null || ticket.getResolutionConfirmedBy() == null) {
            throw new InvalidTicketStateException("Cannot close ticket without resolution confirmation");
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor not found with id: " + principal.getId()));

        ticket.setStatus(TicketStatus.CLOSED);
        Ticket savedTicket = ticketRepository.save(ticket);

        TicketAudit audit = new TicketAudit(savedTicket, actor, AuditAction.TICKET_CLOSED, null);
        ticketAuditRepository.save(audit);

        return TicketDetailResponse.from(savedTicket);
    }
}
