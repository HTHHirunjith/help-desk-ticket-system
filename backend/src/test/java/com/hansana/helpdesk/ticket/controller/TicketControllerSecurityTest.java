package com.hansana.helpdesk.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.common.dto.PagedResponse;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.common.exception.TicketNotEditableException;
import com.hansana.helpdesk.config.SecurityConfig;
import com.hansana.helpdesk.ticket.dto.ChangePriorityRequest;
import com.hansana.helpdesk.ticket.dto.CreateTicketRequest;
import com.hansana.helpdesk.ticket.dto.TicketDetailResponse;
import com.hansana.helpdesk.ticket.dto.TicketSummaryResponse;
import com.hansana.helpdesk.ticket.dto.UpdateTicketRequest;
import com.hansana.helpdesk.ticket.entity.TicketPriority;
import com.hansana.helpdesk.ticket.entity.TicketStatus;
import com.hansana.helpdesk.ticket.service.TicketService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class TicketControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private JwtService jwtService;

    private final UUID sampleId = UUID.randomUUID();
    private final CategoryResponse sampleCat = new CategoryResponse(UUID.randomUUID(), "NETWORK", "Net issues", true);
    private final TicketSummaryResponse.UserRef sampleUserRef = new TicketSummaryResponse.UserRef(UUID.randomUUID(), "Jane Doe");

    private TicketDetailResponse createSampleTicketDetail() {
        return new TicketDetailResponse(
                sampleId,
                1001L,
                "Network down",
                "Cannot access internet",
                sampleCat,
                TicketPriority.HIGH,
                TicketStatus.OPEN,
                sampleUserRef,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Nested
    class CreateTicketEndpointTests {

        @Test
        void unauthenticatedCannotCreateTicket() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest("Title", "Desc", UUID.randomUUID(), TicketPriority.LOW);

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void supportAgentCannotCreateTicket_Forbidden403() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest("Title", "Desc", UUID.randomUUID(), TicketPriority.LOW);

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void adminCannotCreateTicket_Forbidden403() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest("Title", "Desc", UUID.randomUUID(), TicketPriority.LOW);

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userCanCreateTicket_Returns201() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest("Title", "Desc", UUID.randomUUID(), TicketPriority.HIGH);
            when(ticketService.createTicket(any(CreateTicketRequest.class), any())).thenReturn(createSampleTicketDetail());

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(sampleId.toString())))
                    .andExpect(jsonPath("$.title", is("Network down")));
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void missingPriorityReturns400() throws Exception {
            String jsonWithoutPriority = "{\"title\":\"Title\",\"description\":\"Desc\",\"categoryId\":\"" + UUID.randomUUID() + "\"}";

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithoutPriority))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Bad Request")));
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void invalidPriorityEnumReturns400() throws Exception {
            String jsonWithInvalidPriority = "{\"title\":\"Title\",\"description\":\"Desc\",\"categoryId\":\"" + UUID.randomUUID() + "\",\"priority\":\"SUPER_CRITICAL\"}";

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithInvalidPriority))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Bad Request")));
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void missingTitleReturns400() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest("", "Desc", UUID.randomUUID(), TicketPriority.LOW);

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ListAndGetTicketEndpointTests {

        @Test
        void unauthenticatedCannotListTickets() throws Exception {
            mockMvc.perform(get("/api/v1/tickets"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void authenticatedUserCanListTickets() throws Exception {
            PagedResponse<TicketSummaryResponse> emptyPage = new PagedResponse<>(List.of(), 0, 20, 0, 0, true);
            when(ticketService.listTickets(any(), any(), any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void getTicketReturnsTicketWhenFound() throws Exception {
            when(ticketService.getTicket(eq(sampleId), any())).thenReturn(createSampleTicketDetail());

            mockMvc.perform(get("/api/v1/tickets/" + sampleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(sampleId.toString())));
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void getTicketReturns404WhenHiddenOrNotFound() throws Exception {
            when(ticketService.getTicket(eq(sampleId), any()))
                    .thenThrow(new ResourceNotFoundException("Ticket not found"));

            mockMvc.perform(get("/api/v1/tickets/" + sampleId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    @Nested
    class UpdateTicketEndpointTests {

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userCanUpdateOpenTicket() throws Exception {
            UpdateTicketRequest req = new UpdateTicketRequest("Updated Title", null, null);
            when(ticketService.updateOpenTicket(eq(sampleId), any(), any())).thenReturn(createSampleTicketDetail());

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void updateNonOpenTicketReturns409Conflict() throws Exception {
            UpdateTicketRequest req = new UpdateTicketRequest("Updated Title", null, null);
            when(ticketService.updateOpenTicket(eq(sampleId), any(), any()))
                    .thenThrow(new TicketNotEditableException("Ticket is not open"));

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")));
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void agentCannotUpdateTicketDetailsDirectly_Forbidden403() throws Exception {
            UpdateTicketRequest req = new UpdateTicketRequest("Updated Title", null, null);

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ChangePriorityEndpointTests {

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userCannotChangePriority_Forbidden403() throws Exception {
            ChangePriorityRequest req = new ChangePriorityRequest(TicketPriority.URGENT);

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId + "/priority")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void agentCanChangePriority() throws Exception {
            ChangePriorityRequest req = new ChangePriorityRequest(TicketPriority.URGENT);
            when(ticketService.changePriority(eq(sampleId), any(), any())).thenReturn(createSampleTicketDetail());

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId + "/priority")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void adminCanChangePriority() throws Exception {
            ChangePriorityRequest req = new ChangePriorityRequest(TicketPriority.LOW);
            when(ticketService.changePriority(eq(sampleId), any(), any())).thenReturn(createSampleTicketDetail());

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId + "/priority")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void missingPriorityInChangeRequestReturns400() throws Exception {
            String jsonWithoutPriority = "{}";

            mockMvc.perform(patch("/api/v1/tickets/" + sampleId + "/priority")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithoutPriority))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }
}

