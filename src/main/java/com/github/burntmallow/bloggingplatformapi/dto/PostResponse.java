package com.github.burntmallow.bloggingplatformapi.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record PostResponse(
        Long id,
        String title,
        String content,
        String category,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
