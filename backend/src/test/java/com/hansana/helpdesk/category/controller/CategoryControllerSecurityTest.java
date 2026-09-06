package com.hansana.helpdesk.category.controller;

import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import com.hansana.helpdesk.auth.security.JwtService;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.service.CategoryService;
import com.hansana.helpdesk.common.exception.GlobalExceptionHandler;
import com.hansana.helpdesk.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
class CategoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtService jwtService;

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

