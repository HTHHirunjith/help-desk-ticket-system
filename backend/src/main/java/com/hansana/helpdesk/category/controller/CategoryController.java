package com.hansana.helpdesk.category.controller;

import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.category.dto.CreateCategoryRequest;
import com.hansana.helpdesk.category.dto.UpdateCategoryRequest;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories(
            @RequestParam(name = "active", required = false) Boolean active) {
        List<Category> categories = categoryService.findCategories(active);
        return ResponseEntity.ok(categories.stream().map(CategoryResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable("categoryId") UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{categoryId}/activate")
    public ResponseEntity<CategoryResponse> activateCategory(@PathVariable("categoryId") UUID categoryId) {
        CategoryResponse response = categoryService.activateCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{categoryId}/deactivate")
    public ResponseEntity<CategoryResponse> deactivateCategory(@PathVariable("categoryId") UUID categoryId) {
        CategoryResponse response = categoryService.deactivateCategory(categoryId);
        return ResponseEntity.ok(response);
    }
}
