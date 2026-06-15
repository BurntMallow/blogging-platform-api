package com.github.burntmallow.bloggingplatformapi.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.burntmallow.bloggingplatformapi.dto.PostRequest;
import com.github.burntmallow.bloggingplatformapi.dto.PostResponse;
import com.github.burntmallow.bloggingplatformapi.service.PostService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PostController.class)
public class PostControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @Test
    void shouldCreatePostAndReturn201Created() throws Exception {
        // Arrange
        PostRequest request = new PostRequest("Title", "This is the content.", "Category", List.of("old", "new"));
        PostResponse response = new PostResponse(1L, "Title", "This is the content.", "Category", Set.of("old", "new"),
                null, null);

        when(postService.createPost(any(PostRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.content").value("This is the content."))
                .andExpect(jsonPath("$.category").value("Category"))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags").value(containsInAnyOrder("old", "new")));
    }

    @Test
    void shouldReturn400BadRequestWhenRequestIsBlank() throws Exception {
        String invalidRequestJson = """
                {
                }
                """;

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Title is required"))
                .andExpect(jsonPath("$.content").value("Content is required"))
                .andExpect(jsonPath("$.category").value("Category is required"))
                .andExpect(jsonPath("$.tags").value("At least one tag is required"));

        verifyNoInteractions(postService);
    }

    @Test
    void shouldReturn400BadRequestWhenTagisBlank() throws Exception {
        String invalidRequestJson = """
                {
                    "title": "Title",
                    "content": "This is valid content.",
                    "category": "Category",
                    "tags": ["test", " "]
                }
                """;

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['tags[1]']").value("A tag cannot be blank"));

        verifyNoInteractions(postService);
    }
}
