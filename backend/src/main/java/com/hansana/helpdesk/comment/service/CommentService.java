package com.hansana.helpdesk.comment.service;

import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import com.hansana.helpdesk.audit.repository.TicketAuditRepository;
import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.comment.dto.CommentResponse;
import com.hansana.helpdesk.comment.dto.CreateCommentRequest;
import com.hansana.helpdesk.comment.entity.Comment;
import com.hansana.helpdesk.comment.repository.CommentRepository;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketAuditRepository ticketAuditRepository;

    public CommentService(CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository,
                          TicketAuditRepository ticketAuditRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketAuditRepository = ticketAuditRepository;
    }

    /**
     * List comments for a ticket. Access is gated by the same visibility rules as the ticket itself:
     * - USER: only their own tickets
     * - SUPPORT_AGENT: only their assigned tickets
     * - ADMIN: any ticket
     *
     * Returns comments ordered by createdAt ASC (oldest first).
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID ticketId, UserPrincipal principal) {
        Ticket ticket = loadTicketWithVisibilityCheck(ticketId, principal);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Add a comment to a ticket. Access is gated by the same visibility rules as the ticket itself.
     * The author is always derived from the authenticated principal; the client cannot supply an author ID.
     * A COMMENT_ADDED audit event is persisted in the same transaction.
     */
    public CommentResponse addComment(UUID ticketId, CreateCommentRequest request, UserPrincipal principal) {
        Ticket ticket = loadTicketWithVisibilityCheck(ticketId, principal);

        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        Comment comment = new Comment(ticket, author, request.body());
        Comment savedComment = commentRepository.save(comment);

        // Persist COMMENT_ADDED audit event in the same transaction
        TicketAudit audit = new TicketAudit(ticket, author, AuditAction.COMMENT_ADDED, null);
        ticketAuditRepository.save(audit);

        return CommentResponse.from(savedComment);
    }

    /**
     * Loads a ticket and enforces the standard role-based visibility policy:
     * - USER: requester ownership; 404 if not owner or nonexistent
     * - SUPPORT_AGENT: assigned-agent ownership; 404 if not assigned or nonexistent
     * - ADMIN: unrestricted; 404 only if nonexistent
     */
    private Ticket loadTicketWithVisibilityCheck(UUID ticketId, UserPrincipal principal) {
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
        // ADMIN: no additional check needed

        return ticket;
    }
}
