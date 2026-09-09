package com.hansana.helpdesk.category.service;

import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.category.dto.CreateCategoryRequest;
import com.hansana.helpdesk.category.dto.UpdateCategoryRequest;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.common.exception.CategoryAlreadyExistsException;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void findCategoriesWithActiveTrueReturnsOnlyActive() {
        Category cat = new Category("GENERAL", "General inquiries");
        when(categoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(cat));

        List<Category> result = categoryService.findCategories(true);

        assertEquals(1, result.size());
        verify(categoryRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void findCategoriesWithActiveFalseReturnsOnlyInactive() {
        Category cat = new Category("OLD", "Old inquiries");
        cat.setActive(false);
        when(categoryRepository.findByActiveFalseOrderByNameAsc()).thenReturn(List.of(cat));

        List<Category> result = categoryService.findCategories(false);

        assertEquals(1, result.size());
        verify(categoryRepository).findByActiveFalseOrderByNameAsc();
    }

    @Test
    void findCategoriesWithNullActiveReturnsAll() {
        Category cat = new Category("GENERAL", "General inquiries");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cat));

        List<Category> result = categoryService.findCategories(null);

        assertEquals(1, result.size());
        verify(categoryRepository).findAllByOrderByNameAsc();
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

    @Test
    void createCategorySuccessNormalizesAndSetsActiveTrue() {
        CreateCategoryRequest request = new CreateCategoryRequest("  HARDWARE  ", "  Hardware devices  ");
        when(categoryRepository.existsByNameIgnoreCase("HARDWARE")).thenReturn(false);

        UUID generatedId = UUID.randomUUID();
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(generatedId);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(generatedId, response.id());
        assertEquals("HARDWARE", response.name());
        assertEquals("Hardware devices", response.description());
        assertTrue(response.active());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("HARDWARE", captor.getValue().getName());
        assertEquals("Hardware devices", captor.getValue().getDescription());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void createCategoryThrowsIllegalArgumentWhenNameBlank() {
        CreateCategoryRequest request = new CreateCategoryRequest("   ", "Description");
        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategoryThrowsCategoryAlreadyExistsWhenDuplicateCaseInsensitive() {
        CreateCategoryRequest request = new CreateCategoryRequest("billing", "Billing");
        when(categoryRepository.existsByNameIgnoreCase("billing")).thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryNameAndDescriptionSuccess() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("OLD_NAME", "Old Description");
        cat.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByNameIgnoreCase("NEW_NAME")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        UpdateCategoryRequest request = new UpdateCategoryRequest(" NEW_NAME ", " New Description ");
        CategoryResponse response = categoryService.updateCategory(id, request);

        assertEquals("NEW_NAME", response.name());
        assertEquals("New Description", response.description());
        verify(categoryRepository).save(cat);
    }

    @Test
    void updateCategorySameNameCaseInsensitiveDoesNotThrowDuplicate() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("HARDWARE", "Old Description");
        cat.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        UpdateCategoryRequest request = new UpdateCategoryRequest("hardware", "Updated Description");
        CategoryResponse response = categoryService.updateCategory(id, request);

        assertEquals("hardware", response.name());
        assertEquals("Updated Description", response.description());
        verify(categoryRepository, never()).existsByNameIgnoreCase(any());
        verify(categoryRepository).save(cat);
    }

    @Test
    void updateCategoryThrowsIllegalArgumentWhenBothFieldsNull() {
        UUID id = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest(null, null);

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(id, request));
    }

    @Test
    void updateCategoryThrowsIllegalArgumentWhenNameBlank() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("HARDWARE", "Old Description");
        cat.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));

        UpdateCategoryRequest request = new UpdateCategoryRequest("   ", null);

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(id, request));
    }

    @Test
    void updateCategoryThrowsCategoryAlreadyExistsWhenNewNameConflicts() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("HARDWARE", "Old Description");
        cat.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByNameIgnoreCase("SOFTWARE")).thenReturn(true);

        UpdateCategoryRequest request = new UpdateCategoryRequest("SOFTWARE", null);

        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.updateCategory(id, request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void activateCategorySetsActiveTrue() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("HARDWARE", "Hardware");
        cat.setId(id);
        cat.setActive(false);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse response = categoryService.activateCategory(id);

        assertTrue(response.active());
        assertTrue(cat.isActive());
        verify(categoryRepository).save(cat);
    }

    @Test
    void deactivateCategorySetsActiveFalse() {
        UUID id = UUID.randomUUID();
        Category cat = new Category("HARDWARE", "Hardware");
        cat.setId(id);
        cat.setActive(true);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse response = categoryService.deactivateCategory(id);

        assertFalse(response.active());
        assertFalse(cat.isActive());
        verify(categoryRepository).save(cat);
    }
}
