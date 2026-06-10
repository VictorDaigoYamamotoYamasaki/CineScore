package com.cinescore.dto;

import com.cinescore.model.Comment;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private String id;
    private String userId;
    private String userName;
    private String commentText;
    private LocalDateTime createdAt;

    public static CommentDTO from(Comment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setUserId(c.getUser().getId());
        dto.setUserName(c.getUser().getName());
        dto.setCommentText(c.getCommentText());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
