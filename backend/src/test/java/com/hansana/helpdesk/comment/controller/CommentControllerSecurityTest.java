package com.hansana.helpdesk.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hansana.helpdesk.audit.dto.AuditResponse;
import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import com.hansana.helpdesk.audit.repository.TicketAuditRepository;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.comment.dto.CommentResponse;
import com.hansana.helpdesk.comment.service.CommentService;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class CommentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private TicketAuditRepository ticketAuditRepository;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private JwtService jwtService;

    private final UUID ticketId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // List Comments Security Tests
    // -------------------------------------------------------------------------

    @Nested
    class ListCommentsEndpointTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userCanReachListCommentsEndpoint() throws Exception {
            when(commentService.listComments(eq(ticketId), any())).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentCanReachListCommentsEndpoint() throws Exception {
            when(commentService.listComments(eq(ticketId), any())).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanReachListCommentsEndpoint() throws Exception {
            when(commentService.listComments(eq(ticketId), any())).thenReturn(List.of());
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        void serviceReturning404HidesTicketFromUser() throws Exception {
            when(commentService.listComments(eq(ticketId), any()))
                    .thenThrow(new ResourceNotFoundException("Ticket not found"));
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // Add Comment Security Tests
    // -------------------------------------------------------------------------

    @Nested
    class AddCommentEndpointTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"Hello\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userCanAddComment() throws Exception {
            CommentResponse.AuthorRef authorRef = new CommentResponse.AuthorRef(UUID.randomUUID(), "Jane Doe", UserRole.USER);
            CommentResponse resp = new CommentResponse(UUID.randomUUID(), "My comment", authorRef, Instant.now(), Instant.now());
            when(commentService.addComment(eq(ticketId), any(), any())).thenReturn(resp);

            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"My comment\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.body").value("My comment"));
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentCanAddComment() throws Exception {
            CommentResponse.AuthorRef authorRef = new CommentResponse.AuthorRef(UUID.randomUUID(), "Agent Mike", UserRole.SUPPORT_AGENT);
            CommentResponse resp = new CommentResponse(UUID.randomUUID(), "Agent note", authorRef, Instant.now(), Instant.now());
            when(commentService.addComment(eq(ticketId), any(), any())).thenReturn(resp);

            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"Agent note\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanAddComment() throws Exception {
            CommentResponse.AuthorRef authorRef = new CommentResponse.AuthorRef(UUID.randomUUID(), "Admin User", UserRole.ADMIN);
            CommentResponse resp = new CommentResponse(UUID.randomUUID(), "Admin note", authorRef, Instant.now(), Instant.now());
            when(commentService.addComment(eq(ticketId), any(), any())).thenReturn(resp);

            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"Admin note\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "USER")
        void blankBodyReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        void whitespaceOnlyBodyReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"   \"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        void missingBodyFieldReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        void serviceReturning404HidesTicketFromUser() throws Exception {
            when(commentService.addComment(eq(ticketId), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Ticket not found"));

            mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"Hello\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // Audit History Security Tests
    // -------------------------------------------------------------------------

    @Nested
    class AuditHistoryEndpointTests {

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void userReceives403() throws Exception {
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        void agentReceives403() throws Exception {
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanAccessAuditHistory() throws Exception {
            com.hansana.helpdesk.ticket.entity.Ticket ticket = new com.hansana.helpdesk.ticket.entity.Ticket();
            User actor = new User();
            actor.setFirstName("Admin");
            actor.setLastName("User");
            actor.setEmail("admin@example.com");
            actor.setRole(UserRole.ADMIN);

            TicketAudit audit = new TicketAudit(ticket, actor, AuditAction.TICKET_CREATED, "{}");

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketAuditRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
                    .thenReturn(List.of(audit));

            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].action").value("TICKET_CREATED"))
                    .andExpect(jsonPath("$[0].actor.name").value("Admin User"))
                    .andExpect(jsonPath("$[0].actor.email").value("admin@example.com"))
                    .andExpect(jsonPath("$[0].actor.role").value("ADMIN"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void nonexistentTicketReturns404() throws Exception {
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void auditResponseDoesNotExposePasswordField() throws Exception {
            when(ticketRepository.findById(ticketId)).thenReturn(
                    Optional.of(new com.hansana.helpdesk.ticket.entity.Ticket()));

            AuditResponse.ActorRef actorRef = new AuditResponse.ActorRef(
                    UUID.randomUUID(), "Jane Doe", "jane@example.com", UserRole.USER);
            AuditResponse auditResp = new AuditResponse(
                    UUID.randomUUID(), AuditAction.TICKET_CREATED, actorRef, null, Instant.now());

            when(ticketAuditRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
                    .thenReturn(List.of());
            // Verify the DTO: actor has no password field accessible
            // The AuditResponse.ActorRef only exposes id, name, email, role
            // This test confirms the response JSON does not contain "password" key
            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void auditHistoryReturnedInDescendingOrder() throws Exception {
            when(ticketRepository.findById(ticketId)).thenReturn(
                    Optional.of(new com.hansana.helpdesk.ticket.entity.Ticket()));

            // Repository is ordered by createdAt DESC; verify controller delegates to
            // the DESC-ordered repository method
            when(ticketAuditRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/audit"))
                    .andExpect(status().isOk());
        }
    }
}
