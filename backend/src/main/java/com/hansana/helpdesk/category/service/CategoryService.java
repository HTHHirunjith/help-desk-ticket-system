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
}
