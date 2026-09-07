package com.hansana.helpdesk.ticket.controller;

import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.common.dto.PagedResponse;
import com.hansana.helpdesk.ticket.dto.ChangePriorityRequest;
import com.hansana.helpdesk.ticket.dto.CreateTicketRequest;
import com.hansana.helpdesk.ticket.dto.TicketDetailResponse;
import com.hansana.helpdesk.ticket.dto.TicketSummaryResponse;
import com.hansana.helpdesk.ticket.dto.UpdateTicketRequest;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.createTicket(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TicketSummaryResponse>> listTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TicketSummaryResponse> response = ticketService.listTickets(principal, status, priority, categoryId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDetailResponse> getTicket(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.getTicket(ticketId, principal);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{ticketId}")
    public ResponseEntity<TicketDetailResponse> updateOpenTicket(
            @PathVariable("ticketId") UUID ticketId,
            @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.updateOpenTicket(ticketId, request, principal);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{ticketId}/priority")
    public ResponseEntity<TicketDetailResponse> changePriority(
            @PathVariable("ticketId") UUID ticketId,
            @Valid @RequestBody ChangePriorityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.changePriority(ticketId, request, principal);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{ticketId}/assignment")
    public ResponseEntity<TicketDetailResponse> assignTicket(
            @PathVariable("ticketId") UUID ticketId,
            @Valid @RequestBody com.hansana.helpdesk.ticket.dto.AssignTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.assignTicket(ticketId, request, principal);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{ticketId}/assignment")
    public ResponseEntity<Void> unassignTicket(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ticketService.unassignTicket(ticketId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ticketId}/start")
    public ResponseEntity<TicketDetailResponse> startWork(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.startWork(ticketId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketDetailResponse> resolveTicket(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.resolveTicket(ticketId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{ticketId}/confirm-resolution")
    public ResponseEntity<TicketDetailResponse> confirmResolution(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.confirmResolution(ticketId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{ticketId}/reject-resolution")
    public ResponseEntity<TicketDetailResponse> rejectResolution(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.rejectResolution(ticketId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{ticketId}/close")
    public ResponseEntity<TicketDetailResponse> closeTicket(
            @PathVariable("ticketId") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDetailResponse response = ticketService.closeTicket(ticketId, principal);
        return ResponseEntity.ok(response);
    }
}
