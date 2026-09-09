package com.hansana.helpdesk.category.service;

import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.repository.CategoryRepository;
import com.hansana.helpdesk.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public List<Category> findActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Category> findCategories(Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            return categoryRepository.findByActiveTrueOrderByNameAsc();
        } else if (Boolean.FALSE.equals(active)) {
            return categoryRepository.findByActiveFalseOrderByNameAsc();
        }
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Optional<Category> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    public Optional<Category> findByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name);
    }

    @Transactional
    public com.hansana.helpdesk.category.dto.CategoryResponse createCategory(com.hansana.helpdesk.category.dto.CreateCategoryRequest request) {
        String normalizedName = request.name() != null ? request.name().trim() : "";
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new com.hansana.helpdesk.common.exception.CategoryAlreadyExistsException("Category with name '" + normalizedName + "' already exists");
        }

        String description = request.description() != null ? request.description().trim() : "";
        Category category = new Category(normalizedName, description);
        Category saved = categoryRepository.save(category);
        return com.hansana.helpdesk.category.dto.CategoryResponse.from(saved);
    }

    @Transactional
    public com.hansana.helpdesk.category.dto.CategoryResponse updateCategory(UUID id, com.hansana.helpdesk.category.dto.UpdateCategoryRequest request) {
        if (request.name() == null && request.description() == null) {
            throw new IllegalArgumentException("At least one field (name or description) must be provided for update");
        }

        Category category = getById(id);

        if (request.name() != null) {
            String normalizedName = request.name().trim();
            if (normalizedName.isEmpty()) {
                throw new IllegalArgumentException("Category name cannot be blank");
            }
            if (!normalizedName.equalsIgnoreCase(category.getName())) {
                if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
                    throw new com.hansana.helpdesk.common.exception.CategoryAlreadyExistsException("Category with name '" + normalizedName + "' already exists");
                }
                category.setName(normalizedName);
            } else {
                category.setName(normalizedName);
            }
        }

        if (request.description() != null) {
            category.setDescription(request.description().trim());
        }

        Category saved = categoryRepository.save(category);
        return com.hansana.helpdesk.category.dto.CategoryResponse.from(saved);
    }

    @Transactional
    public com.hansana.helpdesk.category.dto.CategoryResponse activateCategory(UUID id) {
        Category category = getById(id);
        category.setActive(true);
        Category saved = categoryRepository.save(category);
        return com.hansana.helpdesk.category.dto.CategoryResponse.from(saved);
    }

    @Transactional
    public com.hansana.helpdesk.category.dto.CategoryResponse deactivateCategory(UUID id) {
        Category category = getById(id);
        category.setActive(false);
        Category saved = categoryRepository.save(category);
        return com.hansana.helpdesk.category.dto.CategoryResponse.from(saved);
    }
}
