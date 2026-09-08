package com.hansana.helpdesk.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment body must not be blank")
        @Size(max = 5000, message = "Comment body must not exceed 5000 characters")
        String body
) {
}
