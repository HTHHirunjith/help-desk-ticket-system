package com.hansana.helpdesk.category.service;

import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void findAllReturnsOrderedCategories() {
        Category cat1 = new Category("ACCOUNT", "Account issues");
        Category cat2 = new Category("BILLING", "Billing issues");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cat1, cat2));

        List<Category> result = categoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("ACCOUNT", result.get(0).getName());
        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    void findActiveCategoriesReturnsOnlyActive() {
        Category cat = new Category("GENERAL", "General inquiries");
        when(categoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(cat));

        List<Category> result = categoryService.findActiveCategories();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
        verify(categoryRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void getByIdReturnsCategoryWhenFound() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("TECHNICAL", "Tech issues");
        cat.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));

        Category result = categoryService.getById(id);

        assertNotNull(result);
        assertEquals("TECHNICAL", result.getName());
    }

    @Test
    void getByIdThrowsResourceNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(id));
    }

    @Test
    void findByNameDelegatesToRepository() {
        Category cat = new Category("FEATURE_REQUEST", "Features");
        when(categoryRepository.findByNameIgnoreCase("feature_request")).thenReturn(Optional.of(cat));

        Optional<Category> result = categoryService.findByName("feature_request");

        assertTrue(result.isPresent());
        assertEquals("FEATURE_REQUEST", result.get().getName());
    }
}
