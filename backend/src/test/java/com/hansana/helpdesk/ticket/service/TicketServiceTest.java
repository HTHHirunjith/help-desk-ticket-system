package com.hansana.helpdesk.ticket.service;

import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.common.dto.PagedResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.hansana.helpdesk.audit.repository.TicketAuditRepository ticketAuditRepository;

    private TicketService ticketService;

    private User requester;
    private User otherUser;
    private User agent;
    private User otherAgent;
    private User admin;
    private Category activeCategory;
    private Category inactiveCategory;

    private UserPrincipal requesterPrincipal;
    private UserPrincipal otherUserPrincipal;
    private UserPrincipal agentPrincipal;
    private UserPrincipal otherAgentPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, categoryRepository, userRepository, ticketAuditRepository);

        UUID requesterId = UUID.randomUUID();
        requester = new User();
        requester.setId(requesterId);
        requester.setFirstName("Jane");
        requester.setLastName("Doe");
        requester.setEmail("user@helpdesk.dev");
        requester.setPassword("password");
        requester.setRole(UserRole.USER);
        requesterPrincipal = new UserPrincipal(requesterId, "user@helpdesk.dev", UserRole.USER);

        UUID otherUserId = UUID.randomUUID();
        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setFirstName("John");
        otherUser.setLastName("Other");
        otherUser.setEmail("other@helpdesk.dev");
        otherUser.setPassword("password");
        otherUser.setRole(UserRole.USER);
        otherUserPrincipal = new UserPrincipal(otherUserId, "other@helpdesk.dev", UserRole.USER);

        UUID agentId = UUID.randomUUID();
        agent = new User();
        agent.setId(agentId);
        agent.setFirstName("Agent");
        agent.setLastName("Smith");
        agent.setEmail("agent@helpdesk.dev");
        agent.setPassword("password");
        agent.setRole(UserRole.SUPPORT_AGENT);
        agentPrincipal = new UserPrincipal(agentId, "agent@helpdesk.dev", UserRole.SUPPORT_AGENT);

        UUID otherAgentId = UUID.randomUUID();
        otherAgent = new User();
        otherAgent.setId(otherAgentId);
        otherAgent.setFirstName("Other");
        otherAgent.setLastName("Agent");
        otherAgent.setEmail("otheragent@helpdesk.dev");
        otherAgent.setPassword("password");
        otherAgent.setRole(UserRole.SUPPORT_AGENT);
        otherAgentPrincipal = new UserPrincipal(otherAgentId, "otheragent@helpdesk.dev", UserRole.SUPPORT_AGENT);

        UUID adminId = UUID.randomUUID();
        admin = new User();
        admin.setId(adminId);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@helpdesk.dev");
        admin.setPassword("password");
        admin.setRole(UserRole.ADMIN);
        adminPrincipal = new UserPrincipal(adminId, "admin@helpdesk.dev", UserRole.ADMIN);

        activeCategory = new Category("SOFTWARE", "Software support");
        activeCategory.setId(UUID.randomUUID());
        activeCategory.setActive(true);

        inactiveCategory = new Category("DEPRECATED", "Old category");
        inactiveCategory.setId(UUID.randomUUID());
        inactiveCategory.setActive(false);
    }

    @Nested
    class CreateTicketTests {

        @Test
        void createTicketSuccess() {
            CreateTicketRequest request = new CreateTicketRequest("Printer issue", "Cannot print", activeCategory.getId(), TicketPriority.HIGH);

            when(categoryRepository.findById(activeCategory.getId())).thenReturn(Optional.of(activeCategory));
            when(userRepository.findById(requesterPrincipal.getId())).thenReturn(Optional.of(requester));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            TicketDetailResponse response = ticketService.createTicket(request, requesterPrincipal);

            assertNotNull(response);
            assertEquals("Printer issue", response.title());
            assertEquals(TicketPriority.HIGH, response.priority());
            assertEquals(TicketStatus.OPEN, response.status());
            assertEquals(requesterPrincipal.getId(), response.requester().id());
        }

        @Test
        void createTicketFailsWhenCategoryNotFound() {
            UUID invalidCatId = UUID.randomUUID();
            CreateTicketRequest request = new CreateTicketRequest("Issue", "Desc", invalidCatId, TicketPriority.LOW);
            when(categoryRepository.findById(invalidCatId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request, requesterPrincipal));
            verify(ticketRepository, never()).save(any());
        }

        @Test
        void createTicketFailsWhenCategoryInactive() {
            CreateTicketRequest request = new CreateTicketRequest("Issue", "Desc", inactiveCategory.getId(), TicketPriority.LOW);
            when(categoryRepository.findById(inactiveCategory.getId())).thenReturn(Optional.of(inactiveCategory));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ticketService.createTicket(request, requesterPrincipal));
            assertEquals("Category is not active", ex.getMessage());
            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    class ListTicketsTests {

        @Test
        void userScopeQueriesByRequester() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(requesterPrincipal, null, null, null, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, null, pageable);
        }

        @Test
        void agentScopeQueriesByAssignedAgent() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findByAssignedAgentWithFilters(agentPrincipal.getId(), "OPEN", null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(agentPrincipal, null, TicketStatus.OPEN, null, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findByAssignedAgentWithFilters(agentPrincipal.getId(), "OPEN", null, null, null, pageable);
        }

        @Test
        void adminScopeQueriesAll() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findAllWithFilters(null, "HIGH", null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(adminPrincipal, null, null, TicketPriority.HIGH, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findAllWithFilters(null, "HIGH", null, null, pageable);
        }

        @Test
        void searchTrimsAndEscapesWildcards() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findAllWithFilters(null, null, null, "50!% discount!_issue!!", pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(adminPrincipal, "  50% discount_issue!  ", null, null, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findAllWithFilters(null, null, null, "50!% discount!_issue!!", pageable);
        }

        @Test
        void searchStripsLeadingHash() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, "1001", pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(requesterPrincipal, " #1001 ", null, null, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, "1001", pageable);
        }

        @Test
        void blankOrWhitespaceOrOnlyHashTreatedAsNoSearch() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findAllWithFilters(null, null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            ticketService.listTickets(adminPrincipal, "", null, null, null, pageable);
            ticketService.listTickets(adminPrincipal, "   ", null, null, null, pageable);
            ticketService.listTickets(adminPrincipal, "#", null, null, null, pageable);
            ticketService.listTickets(adminPrincipal, " # ", null, null, null, pageable);

            verify(ticketRepository, times(4)).findAllWithFilters(null, null, null, null, pageable);
        }

        @Test
        void combinedFiltersWithSearchAppliedCorrectly() {
            Pageable pageable = PageRequest.of(0, 15);
            UUID catId = UUID.randomUUID();
            when(ticketRepository.findByAssignedAgentWithFilters(agentPrincipal.getId(), "IN_PROGRESS", "URGENT", catId, "laptop", pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(agentPrincipal, "laptop", TicketStatus.IN_PROGRESS, TicketPriority.URGENT, catId, pageable);

            assertNotNull(res);
            verify(ticketRepository).findByAssignedAgentWithFilters(agentPrincipal.getId(), "IN_PROGRESS", "URGENT", catId, "laptop", pageable);
        }

        @Test
        void userSearchIsConfinedToRequesterScope() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, "confidential", pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            ticketService.listTickets(requesterPrincipal, "confidential", null, null, null, pageable);

            verify(ticketRepository).findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, "confidential", pageable);
            verify(ticketRepository, never()).findAllWithFilters(any(), any(), any(), any(), any());
            verify(ticketRepository, never()).findByAssignedAgentWithFilters(any(), any(), any(), any(), any(), any());
        }

        @Test
        void agentSearchIsConfinedToAssignedAgentScope() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findByAssignedAgentWithFilters(agentPrincipal.getId(), null, null, null, "secret", pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            ticketService.listTickets(agentPrincipal, "secret", null, null, null, pageable);

            verify(ticketRepository).findByAssignedAgentWithFilters(agentPrincipal.getId(), null, null, null, "secret", pageable);
            verify(ticketRepository, never()).findAllWithFilters(any(), any(), any(), any(), any());
            verify(ticketRepository, never()).findByRequesterWithFilters(any(), any(), any(), any(), any(), any());
        }

        @Test
        void paginationAndSortingPreservedInSearchPagedResponse() {
            Pageable pageable = PageRequest.of(2, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"));
            Ticket ticket1 = new Ticket("Ticket 1", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket1.setId(UUID.randomUUID());
            ticket1.setTicketNumber(1001L);

            Page<Ticket> mockPage = new PageImpl<>(List.of(ticket1), pageable, 11);
            when(ticketRepository.findAllWithFilters(null, null, null, "searchterm", PageRequest.of(2, 5)))
                    .thenReturn(mockPage);

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(adminPrincipal, "searchterm", null, null, null, pageable);

            assertNotNull(res);
            assertEquals(2, res.pageNumber());
            assertEquals(5, res.pageSize());
            assertEquals(11, res.totalElements());
            assertEquals(3, res.totalPages());
            assertEquals(1, res.content().size());
            assertEquals(1001L, res.content().get(0).ticketNumber());
        }
    }

    @Nested
    class GetTicketTests {

        @Test
        void ownerCanGetTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicket(ticketId, requesterPrincipal);

            assertNotNull(response);
            assertEquals(ticketId, response.id());
        }

        @Test
        void nonOwnerUserGets404HidingTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicket(ticketId, otherUserPrincipal));
        }

        @Test
        void assignedAgentCanGetTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicket(ticketId, agentPrincipal);

            assertNotNull(response);
        }

        @Test
        void unassignedAgentGets404HidingTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicket(ticketId, agentPrincipal));
        }

        @Test
        void differentlyAssignedAgentGets404HidingTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicket(ticketId, otherAgentPrincipal));
        }

        @Test
        void adminCanGetAnyTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicket(ticketId, adminPrincipal);

            assertNotNull(response);
        }
    }

    @Nested
    class UpdateOpenTicketSecurityOrderingTests {

        @Test
        void step3_NonOwnerGets404EvenIfTicketIsNotOpen() {
            // CRITICAL TEST: Non-owner gets 404 hiding existence, even if ticket is CLOSED or RESOLVED!
            // Demonstrates ownership is checked BEFORE status check!
            UUID ticketId = UUID.randomUUID();
            Ticket closedTicket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            closedTicket.setId(ticketId);
            closedTicket.setStatus(TicketStatus.CLOSED);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(closedTicket));

            UpdateTicketRequest request = new UpdateTicketRequest("New Title", null, null);

            // Must throw ResourceNotFoundException (404), NOT TicketNotEditableException (409)
            assertThrows(ResourceNotFoundException.class,
                    () -> ticketService.updateOpenTicket(ticketId, request, otherUserPrincipal));
        }

        @Test
        void updateOpenTicket_whenUserIsNotRequesterOnOpenTicket_throwsResourceNotFoundException() {
            UUID ticketId = UUID.randomUUID();
            Ticket openTicket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            openTicket.setId(ticketId);
            openTicket.setStatus(TicketStatus.OPEN);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(openTicket));

            UpdateTicketRequest request = new UpdateTicketRequest("Hacked Title", null, null);

            assertThrows(ResourceNotFoundException.class,
                    () -> ticketService.updateOpenTicket(ticketId, request, otherUserPrincipal));
            verify(ticketRepository, never()).save(any());
        }

        @Test
        void step5_OwnerGets409WhenTicketIsNotOpen() {
            UUID ticketId = UUID.randomUUID();
            Ticket inProgressTicket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            inProgressTicket.setId(ticketId);
            inProgressTicket.setStatus(TicketStatus.IN_PROGRESS);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(inProgressTicket));

            UpdateTicketRequest request = new UpdateTicketRequest("New Title", null, null);

            assertThrows(TicketNotEditableException.class,
                    () -> ticketService.updateOpenTicket(ticketId, request, requesterPrincipal));
        }

        @Test
        void step6_OwnerGets400WhenNoEditableFieldsSupplied() {
            UUID ticketId = UUID.randomUUID();
            Ticket openTicket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            openTicket.setId(ticketId);
            openTicket.setStatus(TicketStatus.OPEN);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(openTicket));

            UpdateTicketRequest request = new UpdateTicketRequest(null, null, null);

            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.updateOpenTicket(ticketId, request, requesterPrincipal));
        }

        @Test
        void step7_OwnerGets400WhenUpdatedCategoryInactive() {
            UUID ticketId = UUID.randomUUID();
            Ticket openTicket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            openTicket.setId(ticketId);
            openTicket.setStatus(TicketStatus.OPEN);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(openTicket));
            when(categoryRepository.findById(inactiveCategory.getId())).thenReturn(Optional.of(inactiveCategory));

            UpdateTicketRequest request = new UpdateTicketRequest(null, null, inactiveCategory.getId());

            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.updateOpenTicket(ticketId, request, requesterPrincipal));
        }

        @Test
        void step8_OwnerUpdatesOpenTicketSuccessfully() {
            UUID ticketId = UUID.randomUUID();
            Ticket openTicket = new Ticket("Old Title", "Old Desc", activeCategory, TicketPriority.LOW, requester);
            openTicket.setId(ticketId);
            openTicket.setStatus(TicketStatus.OPEN);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(openTicket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateTicketRequest request = new UpdateTicketRequest("Updated Title", null, null);

            TicketDetailResponse response = ticketService.updateOpenTicket(ticketId, request, requesterPrincipal);

            assertNotNull(response);
            assertEquals("Updated Title", response.title());
            assertEquals("Old Desc", response.description());
        }
    }

    @Nested
    class ChangePriorityTests {

        @Test
        void adminCanChangeAnyTicketPriority() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            ChangePriorityRequest request = new ChangePriorityRequest(TicketPriority.URGENT);

            TicketDetailResponse response = ticketService.changePriority(ticketId, request, adminPrincipal);

            assertEquals(TicketPriority.URGENT, response.priority());
        }

        @Test
        void assignedAgentCanChangePriority() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            ChangePriorityRequest request = new ChangePriorityRequest(TicketPriority.HIGH);

            TicketDetailResponse response = ticketService.changePriority(ticketId, request, agentPrincipal);

            assertEquals(TicketPriority.HIGH, response.priority());
        }

        @Test
        void unassignedAgentCannotChangePriorityGets404() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            ChangePriorityRequest request = new ChangePriorityRequest(TicketPriority.HIGH);

            assertThrows(ResourceNotFoundException.class,
                    () -> ticketService.changePriority(ticketId, request, agentPrincipal));
        }

        @Test
        void differentlyAssignedAgentCannotChangePriorityGets404() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            ChangePriorityRequest request = new ChangePriorityRequest(TicketPriority.HIGH);

            assertThrows(ResourceNotFoundException.class,
                    () -> ticketService.changePriority(ticketId, request, otherAgentPrincipal));
        }

        @Test
        void unchangedPriorityIsNoOp() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.HIGH, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            ChangePriorityRequest request = new ChangePriorityRequest(TicketPriority.HIGH);

            TicketDetailResponse response = ticketService.changePriority(ticketId, request, agentPrincipal);

            assertEquals(TicketPriority.HIGH, response.priority());
            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    class AssignmentTests {

        @Test
        void adminCanAssignUnassignedTicket_CreatesTicketAssignedAudit() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(null);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(admin));
            when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            AssignTicketRequest request = new AssignTicketRequest(agent.getId());
            TicketDetailResponse response = ticketService.assignTicket(ticketId, request, adminPrincipal);

            assertNotNull(response);
            assertEquals(agent.getId(), response.assignedAgent().id());
            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.TICKET_ASSIGNED, captor.getValue().getAction());
            assertEquals(admin, captor.getValue().getActor());
        }

        @Test
        void adminCanReassignTicket_CreatesTicketReassignedAudit() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(admin));
            when(userRepository.findById(otherAgent.getId())).thenReturn(Optional.of(otherAgent));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            AssignTicketRequest request = new AssignTicketRequest(otherAgent.getId());
            TicketDetailResponse response = ticketService.assignTicket(ticketId, request, adminPrincipal);

            assertNotNull(response);
            assertEquals(otherAgent.getId(), response.assignedAgent().id());
            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.TICKET_REASSIGNED, captor.getValue().getAction());
            assertEquals(admin, captor.getValue().getActor());
        }

        @Test
        void adminAssigningSameAgentThrowsConflictAndCreatesNoAudit() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(admin));
            when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));

            AssignTicketRequest request = new AssignTicketRequest(agent.getId());
            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.assignTicket(ticketId, request, adminPrincipal));

            verify(ticketAuditRepository, never()).save(any());
        }

        @Test
        void adminCanUnassignTicket_CreatesTicketUnassignedAudit() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            ticketService.unassignTicket(ticketId, adminPrincipal);

            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.TICKET_UNASSIGNED, captor.getValue().getAction());
            assertEquals(admin, captor.getValue().getActor());
        }

        @Test
        void adminUnassigningAlreadyUnassignedTicketThrowsConflict() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setAssignedAgent(null);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.unassignTicket(ticketId, adminPrincipal));

            verify(ticketAuditRepository, never()).save(any());
        }

        @Test
        void nonAdminCannotAssignOrUnassign() {
            UUID ticketId = UUID.randomUUID();
            AssignTicketRequest request = new AssignTicketRequest(agent.getId());

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.assignTicket(ticketId, request, agentPrincipal));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.assignTicket(ticketId, request, requesterPrincipal));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.unassignTicket(ticketId, agentPrincipal));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.unassignTicket(ticketId, requesterPrincipal));
        }

        @Test
        void cannotAssignToUserOrAdminOrInactive() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(admin));

            User inactiveAgent = new User();
            inactiveAgent.setId(UUID.randomUUID());
            inactiveAgent.setRole(UserRole.SUPPORT_AGENT);
            inactiveAgent.setActive(false);
            when(userRepository.findById(inactiveAgent.getId())).thenReturn(Optional.of(inactiveAgent));

            when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.assignTicket(ticketId, new AssignTicketRequest(inactiveAgent.getId()), adminPrincipal));

            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.assignTicket(ticketId, new AssignTicketRequest(requester.getId()), adminPrincipal));

            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.assignTicket(ticketId, new AssignTicketRequest(admin.getId()), adminPrincipal));
        }
    }

    @Nested
    class WorkflowTests {

        @Test
        void assignedAgentStartsOpenTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(agentPrincipal.getId())).thenReturn(Optional.of(agent));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketDetailResponse response = ticketService.startWork(ticketId, agentPrincipal);

            assertEquals(TicketStatus.IN_PROGRESS, response.status());
            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.STATUS_CHANGED, captor.getValue().getAction());
            assertEquals(agent, captor.getValue().getActor());
        }

        @Test
        void unassignedOrWrongAgentOrNonAgentCannotStartWork() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.OPEN);

            // Unassigned ticket -> 404 for agent
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            assertThrows(ResourceNotFoundException.class, () -> ticketService.startWork(ticketId, agentPrincipal));

            // Assigned to another agent -> 404 for different agent
            ticket.setAssignedAgent(otherAgent);
            assertThrows(ResourceNotFoundException.class, () -> ticketService.startWork(ticketId, agentPrincipal));

            // USER and ADMIN -> 403
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.startWork(ticketId, requesterPrincipal));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.startWork(ticketId, adminPrincipal));
        }

        @Test
        void startingNonOpenTicketThrowsConflict() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.startWork(ticketId, agentPrincipal));
        }

        @Test
        void assignedAgentResolvesInProgressTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(agentPrincipal.getId())).thenReturn(Optional.of(agent));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketDetailResponse response = ticketService.resolveTicket(ticketId, agentPrincipal);

            assertEquals(TicketStatus.RESOLVED, response.status());
            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.STATUS_CHANGED, captor.getValue().getAction());
            assertEquals(agent, captor.getValue().getActor());
        }

        @Test
        void resolvingNonInProgressTicketThrowsConflict() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.resolveTicket(ticketId, agentPrincipal));
        }

        @Test
        void resolvingUnassignedTicketOrWrongAgentThrowsResourceNotFoundException() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setAssignedAgent(null); // Unassigned

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            // Unassigned agent attempt -> 404
            assertThrows(ResourceNotFoundException.class, () -> ticketService.resolveTicket(ticketId, agentPrincipal));

            // Assigned to another agent -> 404
            ticket.setAssignedAgent(otherAgent);
            assertThrows(ResourceNotFoundException.class, () -> ticketService.resolveTicket(ticketId, agentPrincipal));
        }

        @Test
        void requesterConfirmsResolvedTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setAssignedAgent(agent);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(requesterPrincipal.getId())).thenReturn(Optional.of(requester));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketDetailResponse response = ticketService.confirmResolution(ticketId, requesterPrincipal);

            assertEquals(TicketStatus.RESOLVED, response.status());
            assertNotNull(response.resolutionConfirmedAt());
            assertEquals(requester.getId(), response.resolutionConfirmedBy().id());
            assertEquals(agent.getId(), response.assignedAgent().id());

            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.RESOLUTION_CONFIRMED, captor.getValue().getAction());
            assertEquals(requester, captor.getValue().getActor());
        }

        @Test
        void nonRequesterOrNonResolvedTicketCannotConfirmResolution() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.RESOLVED);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            // Other user gets 404
            assertThrows(ResourceNotFoundException.class,
                    () -> ticketService.confirmResolution(ticketId, otherUserPrincipal));

            // Agent / Admin get 403
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.confirmResolution(ticketId, agentPrincipal));

            // Non-resolved ticket throws 409 Conflict for requester
            ticket.setStatus(TicketStatus.OPEN);
            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.confirmResolution(ticketId, requesterPrincipal));
        }

        @Test
        void requesterRejectsResolvedTicket_ReopensToOpenAndClearsConfirmation() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setAssignedAgent(agent);
            ticket.setResolutionConfirmedAt(java.time.Instant.now());
            ticket.setResolutionConfirmedBy(requester);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(requesterPrincipal.getId())).thenReturn(Optional.of(requester));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketDetailResponse response = ticketService.rejectResolution(ticketId, requesterPrincipal);

            assertEquals(TicketStatus.OPEN, response.status());
            org.junit.jupiter.api.Assertions.assertNull(response.resolutionConfirmedAt());
            org.junit.jupiter.api.Assertions.assertNull(response.resolutionConfirmedBy());
            assertEquals(agent.getId(), response.assignedAgent().id());

            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.TICKET_REOPENED, captor.getValue().getAction());
            assertEquals(requester, captor.getValue().getActor());
        }

        @Test
        void adminClosesConfirmedResolvedTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolutionConfirmedAt(java.time.Instant.now());
            ticket.setResolutionConfirmedBy(requester);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketDetailResponse response = ticketService.closeTicket(ticketId, adminPrincipal);

            assertEquals(TicketStatus.CLOSED, response.status());

            org.mockito.ArgumentCaptor<com.hansana.helpdesk.audit.entity.TicketAudit> captor =
                    org.mockito.ArgumentCaptor.forClass(com.hansana.helpdesk.audit.entity.TicketAudit.class);
            verify(ticketAuditRepository).save(captor.capture());
            assertEquals(com.hansana.helpdesk.audit.entity.AuditAction.TICKET_CLOSED, captor.getValue().getAction());
            assertEquals(admin, captor.getValue().getActor());
        }

        @Test
        void adminCannotCloseUnconfirmedOrNonResolvedTicket() {
            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket("Title", "Desc", activeCategory, TicketPriority.LOW, requester);
            ticket.setId(ticketId);
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolutionConfirmedAt(null);
            ticket.setResolutionConfirmedBy(null);

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            // Unconfirmed -> 409
            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.closeTicket(ticketId, adminPrincipal));

            // Non-resolved -> 409
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            assertThrows(com.hansana.helpdesk.common.exception.InvalidTicketStateException.class,
                    () -> ticketService.closeTicket(ticketId, adminPrincipal));

            // Non-admin -> 403
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.closeTicket(ticketId, agentPrincipal));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> ticketService.closeTicket(ticketId, requesterPrincipal));
        }
    }
}

