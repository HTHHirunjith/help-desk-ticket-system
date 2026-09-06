package com.hansana.helpdesk.ticket.service;

import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.common.dto.PagedResponse;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.common.exception.TicketNotEditableException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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
            TicketStatus status,
            TicketPriority priority,
            UUID categoryId,
            Pageable pageable) {

        Page<Ticket> page;
        UserRole role = principal.getRole();

        if (role == UserRole.USER) {
            page = ticketRepository.findByRequesterWithFilters(principal.getId(), status, priority, categoryId, pageable);
        } else if (role == UserRole.SUPPORT_AGENT) {
            page = ticketRepository.findByAssignedAgentWithFilters(principal.getId(), status, priority, categoryId, pageable);
        } else {
            page = ticketRepository.findAllWithFilters(status, priority, categoryId, pageable);
        }

        return PagedResponse.from(page.map(TicketSummaryResponse::from));
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
}
