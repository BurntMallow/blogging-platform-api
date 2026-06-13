package com.github.burntmallow.bloggingplatformapi.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record PostRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Content is required") String content,
        @NotBlank(message = "Category is required") String category,
        @NotEmpty(message = "At least one tag is required") List<@NotBlank(message = "A tag cannot be blank") String> tags) {

}
