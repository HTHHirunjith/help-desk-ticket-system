package com.hansana.helpdesk.comment.service;

import com.hansana.helpdesk.audit.entity.AuditAction;
import com.hansana.helpdesk.audit.entity.TicketAudit;
import com.hansana.helpdesk.audit.repository.TicketAuditRepository;
import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.comment.dto.CommentResponse;
import com.hansana.helpdesk.comment.dto.CreateCommentRequest;
import com.hansana.helpdesk.comment.entity.Comment;
import com.hansana.helpdesk.comment.repository.CommentRepository;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.ticket.entity.Ticket;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.ticket.repository.TicketRepository;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketAuditRepository ticketAuditRepository;

    private CommentService commentService;

    // Users
    private User requester;
    private User agent;
    private User otherAgent;
    private User admin;
    private User otherUser;

    // Principals
    private UserPrincipal requesterPrincipal;
    private UserPrincipal agentPrincipal;
    private UserPrincipal otherAgentPrincipal;
    private UserPrincipal adminPrincipal;
    private UserPrincipal otherUserPrincipal;

    // Shared fixture
    private Category category;
    private Ticket assignedTicket;
    private Ticket unassignedTicket;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, ticketRepository, userRepository, ticketAuditRepository);

        UUID requesterId = UUID.randomUUID();
        requester = new User();
        requester.setId(requesterId);
        requester.setFirstName("Jane");
        requester.setLastName("Doe");
        requester.setEmail("user@helpdesk.dev");
        requester.setPassword("hash");
        requester.setRole(UserRole.USER);
        requesterPrincipal = new UserPrincipal(requesterId, "user@helpdesk.dev", UserRole.USER);

        UUID otherUserId = UUID.randomUUID();
        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmail("other@helpdesk.dev");
        otherUser.setPassword("hash");
        otherUser.setRole(UserRole.USER);
        otherUserPrincipal = new UserPrincipal(otherUserId, "other@helpdesk.dev", UserRole.USER);

        UUID agentId = UUID.randomUUID();
        agent = new User();
        agent.setId(agentId);
        agent.setFirstName("Mike");
        agent.setLastName("Agent");
        agent.setEmail("agent@helpdesk.dev");
        agent.setPassword("hash");
        agent.setRole(UserRole.SUPPORT_AGENT);
        agentPrincipal = new UserPrincipal(agentId, "agent@helpdesk.dev", UserRole.SUPPORT_AGENT);

        UUID otherAgentId = UUID.randomUUID();
        otherAgent = new User();
        otherAgent.setId(otherAgentId);
        otherAgent.setFirstName("Alice");
        otherAgent.setLastName("Agent");
        otherAgent.setEmail("agent2@helpdesk.dev");
        otherAgent.setPassword("hash");
        otherAgent.setRole(UserRole.SUPPORT_AGENT);
        otherAgentPrincipal = new UserPrincipal(otherAgentId, "agent2@helpdesk.dev", UserRole.SUPPORT_AGENT);

        UUID adminId = UUID.randomUUID();
        admin = new User();
        admin.setId(adminId);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@helpdesk.dev");
        admin.setPassword("hash");
        admin.setRole(UserRole.ADMIN);
        adminPrincipal = new UserPrincipal(adminId, "admin@helpdesk.dev", UserRole.ADMIN);

        category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("ACCOUNT");
        category.setDescription("Account issues");
        category.setActive(true);

        // Assigned ticket (requester=requester, assignedAgent=agent)
        assignedTicket = new Ticket("Network down", "Cannot connect", category, TicketPriority.HIGH, requester);
        assignedTicket.setId(UUID.randomUUID());
        assignedTicket.setStatus(TicketStatus.IN_PROGRESS);
        assignedTicket.setAssignedAgent(agent);

        // Unassigned ticket (requester=requester, assignedAgent=null)
        unassignedTicket = new Ticket("Slow login", "Login is slow", category, TicketPriority.LOW, requester);
        unassignedTicket.setId(UUID.randomUUID());
        unassignedTicket.setStatus(TicketStatus.OPEN);
    }

    // -------------------------------------------------------------------------
    // List Comments Tests
    // -------------------------------------------------------------------------

    @Nested
    class ListCommentTests {

        @Test
        void userCanViewCommentsOnOwnTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            Comment c = new Comment(assignedTicket, agent, "Test comment");
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(assignedTicket.getId())).thenReturn(List.of(c));

            List<CommentResponse> result = commentService.listComments(assignedTicket.getId(), requesterPrincipal);

            assertEquals(1, result.size());
            assertEquals("Test comment", result.get(0).body());
        }

        @Test
        void userCannotViewCommentsOnAnotherUsersTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));

            // otherUser is not the requester of assignedTicket
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.listComments(assignedTicket.getId(), otherUserPrincipal));
        }

        @Test
        void agentCanViewCommentsOnAssignedTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(assignedTicket.getId())).thenReturn(List.of());

            List<CommentResponse> result = commentService.listComments(assignedTicket.getId(), agentPrincipal);

            assertEquals(0, result.size());
        }

        @Test
        void agentCannotViewCommentsOnAnotherAgentsTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));

            // otherAgent is not the assigned agent
            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.listComments(assignedTicket.getId(), otherAgentPrincipal));
        }

        @Test
        void agentCannotViewCommentsOnUnassignedTicket() {
            when(ticketRepository.findById(unassignedTicket.getId())).thenReturn(Optional.of(unassignedTicket));

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.listComments(unassignedTicket.getId(), agentPrincipal));
        }

        @Test
        void adminCanViewCommentsOnAnyTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(assignedTicket.getId())).thenReturn(List.of());

            List<CommentResponse> result = commentService.listComments(assignedTicket.getId(), adminPrincipal);

            assertEquals(0, result.size());
        }

        @Test
        void nonexistentTicketReturns404() {
            UUID bogusId = UUID.randomUUID();
            when(ticketRepository.findById(bogusId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.listComments(bogusId, adminPrincipal));
        }

        @Test
        void commentsReturnedInCreatedAtAscOrder() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));

            Instant earlier = Instant.now().minusSeconds(60);
            Instant later = Instant.now();

            Comment c1 = new Comment(assignedTicket, requester, "First comment");
            c1.setId(UUID.randomUUID());
            c1.setCreatedAt(earlier);
            c1.setUpdatedAt(earlier);

            Comment c2 = new Comment(assignedTicket, agent, "Second comment");
            c2.setId(UUID.randomUUID());
            c2.setCreatedAt(later);
            c2.setUpdatedAt(later);

            // Repository returns in createdAt ASC order (as declared in the method name)
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(assignedTicket.getId()))
                    .thenReturn(List.of(c1, c2));

            List<CommentResponse> result = commentService.listComments(assignedTicket.getId(), requesterPrincipal);

            assertEquals(2, result.size());
            assertEquals("First comment", result.get(0).body());
            assertEquals("Second comment", result.get(1).body());
        }
    }

    // -------------------------------------------------------------------------
    // Add Comment Tests
    // -------------------------------------------------------------------------

    @Nested
    class AddCommentTests {

        @Test
        void userCanAddCommentToOwnTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));

            Comment saved = new Comment(assignedTicket, requester, "My comment");
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            when(commentRepository.save(any(Comment.class))).thenReturn(saved);

            CreateCommentRequest req = new CreateCommentRequest("My comment");
            CommentResponse response = commentService.addComment(assignedTicket.getId(), req, requesterPrincipal);

            assertEquals("My comment", response.body());
            assertEquals(requester.getId(), response.author().id());
            assertEquals(UserRole.USER, response.author().role());
        }

        @Test
        void userCannotAddCommentToAnotherUsersTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.addComment(assignedTicket.getId(),
                            new CreateCommentRequest("body"), otherUserPrincipal));

            verify(commentRepository, never()).save(any());
        }

        @Test
        void agentCanAddCommentToAssignedTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));

            Comment saved = new Comment(assignedTicket, agent, "Agent note");
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            when(commentRepository.save(any(Comment.class))).thenReturn(saved);

            CommentResponse response = commentService.addComment(assignedTicket.getId(),
                    new CreateCommentRequest("Agent note"), agentPrincipal);

            assertEquals("Agent note", response.body());
            assertEquals(UserRole.SUPPORT_AGENT, response.author().role());
        }

        @Test
        void agentCannotAddCommentToAnotherAgentsTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.addComment(assignedTicket.getId(),
                            new CreateCommentRequest("body"), otherAgentPrincipal));

            verify(commentRepository, never()).save(any());
        }

        @Test
        void agentCannotAddCommentToUnassignedTicket() {
            when(ticketRepository.findById(unassignedTicket.getId())).thenReturn(Optional.of(unassignedTicket));

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.addComment(unassignedTicket.getId(),
                            new CreateCommentRequest("body"), agentPrincipal));

            verify(commentRepository, never()).save(any());
        }

        @Test
        void adminCanAddCommentToAnyTicket() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

            Comment saved = new Comment(assignedTicket, admin, "Admin note");
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            when(commentRepository.save(any(Comment.class))).thenReturn(saved);

            CommentResponse response = commentService.addComment(assignedTicket.getId(),
                    new CreateCommentRequest("Admin note"), adminPrincipal);

            assertEquals("Admin note", response.body());
            assertEquals(UserRole.ADMIN, response.author().role());
        }

        @Test
        void authorAlwaysComeFromAuthenticatedPrincipal() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));

            ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
            Comment saved = new Comment(assignedTicket, requester, "Body");
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            when(commentRepository.save(commentCaptor.capture())).thenReturn(saved);

            commentService.addComment(assignedTicket.getId(),
                    new CreateCommentRequest("Body"), requesterPrincipal);

            // Author must be the authenticated user loaded from the repository
            assertEquals(requester.getId(), commentCaptor.getValue().getAuthor().getId());
        }

        @Test
        void commentAddedCreatesCommentAddedAuditEvent() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));
            when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));

            Comment saved = new Comment(assignedTicket, requester, "Audit test");
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            when(commentRepository.save(any(Comment.class))).thenReturn(saved);

            commentService.addComment(assignedTicket.getId(),
                    new CreateCommentRequest("Audit test"), requesterPrincipal);

            ArgumentCaptor<TicketAudit> auditCaptor = ArgumentCaptor.forClass(TicketAudit.class);
            verify(ticketAuditRepository).save(auditCaptor.capture());
            assertEquals(AuditAction.COMMENT_ADDED, auditCaptor.getValue().getAction());
            // Actor must be the authenticated requester
            assertEquals(requester.getId(), auditCaptor.getValue().getActor().getId());
        }

        @Test
        void noAuditEventCreatedIfCommentRejectedByVisibilityCheck() {
            when(ticketRepository.findById(assignedTicket.getId())).thenReturn(Optional.of(assignedTicket));

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.addComment(assignedTicket.getId(),
                            new CreateCommentRequest("body"), otherUserPrincipal));

            verify(ticketAuditRepository, never()).save(any());
            verify(commentRepository, never()).save(any());
        }
    }
}
