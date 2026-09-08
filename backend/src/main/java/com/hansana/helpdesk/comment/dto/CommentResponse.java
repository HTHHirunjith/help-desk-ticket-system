package com.hansana.helpdesk.comment.dto;

import com.hansana.helpdesk.comment.entity.Comment;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String body,
        AuthorRef author,
        Instant createdAt,
        Instant updatedAt
) {
    public record AuthorRef(UUID id, String name, UserRole role) {
        public static AuthorRef from(User user) {
            if (user == null) {
                return null;
            }
            return new AuthorRef(
                    user.getId(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getRole()
            );
        }
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                AuthorRef.from(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
