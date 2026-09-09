package com.hansana.helpdesk.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.category.dto.CreateCategoryRequest;
import com.hansana.helpdesk.category.dto.UpdateCategoryRequest;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.service.CategoryService;
import com.hansana.helpdesk.common.exception.CategoryAlreadyExistsException;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import com.hansana.helpdesk.config.SecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
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

@WebMvcTest(controllers = CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class CategoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtService jwtService;

    @Nested
    class ListCategoriesEndpointTests {
        @Test
        void unauthenticatedAccessReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void authenticatedUserCanListCategories() throws Exception {
            Category c = new Category("HARDWARE", "Hardware issues");
            when(categoryService.findCategories(null)).thenReturn(List.of(c));

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("HARDWARE")));

            verify(categoryService).findCategories(null);
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void authenticatedUserCanFilterActiveCategories() throws Exception {
            Category c = new Category("SOFTWARE", "Software issues");
            when(categoryService.findCategories(true)).thenReturn(List.of(c));

            mockMvc.perform(get("/api/v1/categories").param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("SOFTWARE")));

            verify(categoryService).findCategories(true);
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void authenticatedUserCanFilterInactiveCategories() throws Exception {
            Category c = new Category("OLD", "Old issues");
            c.setActive(false);
            when(categoryService.findCategories(false)).thenReturn(List.of(c));

            mockMvc.perform(get("/api/v1/categories").param("active", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("OLD")));

            verify(categoryService).findCategories(false);
        }
    }

    @Nested
    class CreateCategoryEndpointTests {
        @Test
        void unauthenticatedReturns401() throws Exception {
            CreateCategoryRequest req = new CreateCategoryRequest("NETWORKING", "Network issues");
            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userRoleReturns403() throws Exception {
            CreateCategoryRequest req = new CreateCategoryRequest("NETWORKING", "Network issues");
            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void agentRoleReturns403() throws Exception {
            CreateCategoryRequest req = new CreateCategoryRequest("NETWORKING", "Network issues");
            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void adminRoleCanCreateCategory() throws Exception {
            UUID id = UUID.randomUUID();
            CreateCategoryRequest req = new CreateCategoryRequest("NETWORKING", "Network issues");
            CategoryResponse res = new CategoryResponse(id, "NETWORKING", "Network issues", true);

            when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(res);

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(id.toString())))
                    .andExpect(jsonPath("$.name", is("NETWORKING")))
                    .andExpect(jsonPath("$.active", is(true)));
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void blankNameReturns400() throws Exception {
            CreateCategoryRequest req = new CreateCategoryRequest("", "Network issues");
            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void duplicateCategoryReturns409() throws Exception {
            CreateCategoryRequest req = new CreateCategoryRequest("BILLING", "Billing");
            when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                    .thenThrow(new CategoryAlreadyExistsException("Category with name 'BILLING' already exists"));

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("Category with name 'BILLING' already exists")));
        }
    }

    @Nested
    class UpdateCategoryEndpointTests {
        @Test
        void unauthenticatedReturns401() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest req = new UpdateCategoryRequest("NEW_NAME", null);
            mockMvc.perform(patch("/api/v1/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userRoleReturns403() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest req = new UpdateCategoryRequest("NEW_NAME", null);
            mockMvc.perform(patch("/api/v1/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void agentRoleReturns403() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest req = new UpdateCategoryRequest("NEW_NAME", null);
            mockMvc.perform(patch("/api/v1/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void adminRoleCanUpdateCategory() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest req = new UpdateCategoryRequest("UPDATED_NAME", "Updated description");
            CategoryResponse res = new CategoryResponse(id, "UPDATED_NAME", "Updated description", true);

            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class))).thenReturn(res);

            mockMvc.perform(patch("/api/v1/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("UPDATED_NAME")))
                    .andExpect(jsonPath("$.description", is("Updated description")));
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void conflictReturns409() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest req = new UpdateCategoryRequest("CONFLICT_NAME", null);
            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class)))
                    .thenThrow(new CategoryAlreadyExistsException("Category with name 'CONFLICT_NAME' already exists"));

            mockMvc.perform(patch("/api/v1/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void notFoundReturns404() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateCategoryRequest req = new UpdateCategoryRequest("NAME", null);
            when(categoryService.updateCategory(eq(id), any(UpdateCategoryRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Category not found with id: " + id));

            mockMvc.perform(patch("/api/v1/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ActivateCategoryEndpointTests {
        @Test
        void unauthenticatedReturns401() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/categories/" + id + "/activate"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userRoleReturns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/categories/" + id + "/activate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void agentRoleReturns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/categories/" + id + "/activate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void adminRoleCanActivateCategory() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryResponse res = new CategoryResponse(id, "ACCOUNT", "Description", true);
            when(categoryService.activateCategory(id)).thenReturn(res);

            mockMvc.perform(post("/api/v1/categories/" + id + "/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(true)));
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void activateNotFoundReturns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.activateCategory(id))
                    .thenThrow(new ResourceNotFoundException("Category not found with id: " + id));

            mockMvc.perform(post("/api/v1/categories/" + id + "/activate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeactivateCategoryEndpointTests {
        @Test
        void unauthenticatedReturns401() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/categories/" + id + "/deactivate"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user@helpdesk.dev", roles = {"USER"})
        void userRoleReturns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/categories/" + id + "/deactivate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "agent@helpdesk.dev", roles = {"SUPPORT_AGENT"})
        void agentRoleReturns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/categories/" + id + "/deactivate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void adminRoleCanDeactivateCategory() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryResponse res = new CategoryResponse(id, "ACCOUNT", "Description", false);
            when(categoryService.deactivateCategory(id)).thenReturn(res);

            mockMvc.perform(post("/api/v1/categories/" + id + "/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(false)));
        }

        @Test
        @WithMockUser(username = "admin@helpdesk.dev", roles = {"ADMIN"})
        void deactivateNotFoundReturns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.deactivateCategory(id))
                    .thenThrow(new ResourceNotFoundException("Category not found with id: " + id));

            mockMvc.perform(post("/api/v1/categories/" + id + "/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }
}
