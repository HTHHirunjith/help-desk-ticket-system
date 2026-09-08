package com.hansana.helpdesk.comment.controller;

import com.hansana.helpdesk.audit.dto.AuditResponse;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import com.hansana.helpdesk.audit.repository.TicketAuditRepository;
import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.comment.dto.CommentResponse;
import com.hansana.helpdesk.comment.dto.CreateCommentRequest;
import com.hansana.helpdesk.comment.service.CommentService;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tickets")
public class CommentController {

    private final CommentService commentService;
    private final TicketAuditRepository ticketAuditRepository;
    private final TicketRepository ticketRepository;

    public CommentController(CommentService commentService,
                             TicketAuditRepository ticketAuditRepository,
                             TicketRepository ticketRepository) {
        this.commentService = commentService;
        this.ticketAuditRepository = ticketAuditRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * GET /api/v1/tickets/{ticketId}/comments
     * Access: same ticket visibility rules (USER=own, AGENT=assigned, ADMIN=any).
     * Returns comments ordered createdAt ASC.
     */
    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CommentResponse> comments = commentService.listComments(ticketId, principal);
        return ResponseEntity.ok(comments);
    }

    /**
     * POST /api/v1/tickets/{ticketId}/comments
     * Access: USER=own ticket, SUPPORT_AGENT=assigned ticket, ADMIN=any ticket.
     * Author is always the authenticated principal. Client may not supply authorId.
     * Creates COMMENT_ADDED audit event in the same transaction.
     */
    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable("ticketId") UUID ticketId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CommentResponse response = commentService.addComment(ticketId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/tickets/{ticketId}/audit
     * Access: ADMIN only (enforced at URL level in SecurityConfig and by service ADMIN role check).
     * Returns audit history for the ticket ordered createdAt DESC.
     */
    @GetMapping("/{ticketId}/audit")
    public ResponseEntity<List<AuditResponse>> getAuditHistory(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify ticket exists (returns 404 for nonexistent tickets)
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        List<TicketAudit> audits = ticketAuditRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        List<AuditResponse> response = audits.stream()
                .map(AuditResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
