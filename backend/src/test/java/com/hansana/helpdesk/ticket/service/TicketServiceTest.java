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

    private TicketService ticketService;

    private User requester;
    private User otherUser;
    private User agent;
    private User otherAgent;
    private Category activeCategory;
    private Category inactiveCategory;

    private UserPrincipal requesterPrincipal;
    private UserPrincipal otherUserPrincipal;
    private UserPrincipal agentPrincipal;
    private UserPrincipal otherAgentPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, categoryRepository, userRepository);

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
            when(ticketRepository.findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(requesterPrincipal, null, null, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findByRequesterWithFilters(requesterPrincipal.getId(), null, null, null, pageable);
        }

        @Test
        void agentScopeQueriesByAssignedAgent() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findByAssignedAgentWithFilters(agentPrincipal.getId(), TicketStatus.OPEN, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(agentPrincipal, TicketStatus.OPEN, null, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findByAssignedAgentWithFilters(agentPrincipal.getId(), TicketStatus.OPEN, null, null, pageable);
        }

        @Test
        void adminScopeQueriesAll() {
            Pageable pageable = PageRequest.of(0, 10);
            when(ticketRepository.findAllWithFilters(null, TicketPriority.HIGH, null, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponse<TicketSummaryResponse> res = ticketService.listTickets(adminPrincipal, null, TicketPriority.HIGH, null, pageable);

            assertNotNull(res);
            verify(ticketRepository).findAllWithFilters(null, TicketPriority.HIGH, null, pageable);
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
}

