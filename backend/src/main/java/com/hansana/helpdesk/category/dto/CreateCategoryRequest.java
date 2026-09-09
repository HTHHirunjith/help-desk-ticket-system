package com.hansana.helpdesk.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
    public CreateCategoryRequest {
        name = name != null ? name.trim() : null;
        description = description != null ? description.trim() : "";
    }
}
