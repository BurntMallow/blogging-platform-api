package com.github.burntmallow.bloggingplatformapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String content,
        String category,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
