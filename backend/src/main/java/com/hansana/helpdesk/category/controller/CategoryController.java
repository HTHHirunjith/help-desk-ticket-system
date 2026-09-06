package com.hansana.helpdesk.category.controller;

import com.hansana.helpdesk.category.dto.CategoryResponse;
import com.hansana.helpdesk.category.entity.Category;
import com.hansana.helpdesk.category.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
