package com.github.burntmallow.bloggingplatformapi.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import com.github.burntmallow.bloggingplatformapi.dto.PostRequest;
import com.github.burntmallow.bloggingplatformapi.dto.PostResponse;
import com.github.burntmallow.bloggingplatformapi.service.PostService;

@WebMvcTest(PostController.class)
public class PostControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    private static final String TITLE = "Title";
    private static final String CONTENT = "This is the content.";
    private static final String CATEGORY = "Category";
    private static final String OLD_TAG_NAME = "old";
    private static final String NEW_TAG_NAME = "new";
    private static final Long POST_ID = 1L;

    private final String validRequestJson = """
            {
                "title": "Title",
                "content": "This is the content.",
                "category": "Category",
                "tags": ["old", "new"]
            }
            """;

    private PostResponse createExpectedResponse() {
        return new PostResponse(POST_ID, TITLE, CONTENT, CATEGORY, Set.of(OLD_TAG_NAME, NEW_TAG_NAME), null, null);
    }

    private void executeAndVerify(
            MockHttpServletRequestBuilder requestBuilder,
            String jsonPayload,
            ResultMatcher expectedStatus,
            ResultMatcher... expectedJsonPaths) throws Exception {

        if (jsonPayload != null) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON).content(jsonPayload);
        }

        var responseActions = mockMvc.perform(requestBuilder).andExpect(expectedStatus);

        for (ResultMatcher pathMatcher : expectedJsonPaths) {
            responseActions.andExpect(pathMatcher);
        }
    }

    @Test
    void shouldReturnSavedPostDetailsOnSuccess() throws Exception {
        PostResponse expectedResponse = createExpectedResponse();

        when(postService.createPost(any(PostRequest.class))).thenReturn(expectedResponse);
        when(postService.updatePost(any(PostRequest.class), eq(POST_ID))).thenReturn(expectedResponse);

        ResultMatcher[] postDetailsAssertions = {
                jsonPath("$.id").value(POST_ID),
                jsonPath("$.title").value(TITLE),
                jsonPath("$.content").value(CONTENT),
                jsonPath("$.category").value(CATEGORY),
                jsonPath("$.tags").isArray(),
                jsonPath("$.tags").value(containsInAnyOrder(OLD_TAG_NAME, NEW_TAG_NAME))
        };

        executeAndVerify(post("/api/posts"), validRequestJson, status().isCreated(), postDetailsAssertions);
        executeAndVerify(put("/api/posts/{id}", POST_ID), validRequestJson, status().isOk(), postDetailsAssertions);
    }

    @Test
    void shouldReturnNothingOnSuccessfulDelete() throws Exception {
        doNothing().when(postService).deletePost(eq(POST_ID));

        executeAndVerify(delete("/api/posts/{id}", POST_ID), null, status().isNoContent());
    }

    @Test
    void shouldReturn404WhenResourceNotFound() throws Exception {
        ResultMatcher notFoundAssertion = jsonPath("$.error").value("Blog post not found");

        when(postService.updatePost(any(PostRequest.class), eq(POST_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"))
                .when(postService).deletePost(eq(POST_ID));

        executeAndVerify(put("/api/posts/{id}", POST_ID), validRequestJson, status().isNotFound(), notFoundAssertion);
        executeAndVerify(delete("/api/posts/{id}", POST_ID), null, status().isNotFound(), notFoundAssertion);

        verify(postService).updatePost(any(PostRequest.class), eq(POST_ID));
    }

    @Test
    void shouldReturnSpecificErrorWhenTagIndexIsBlank() throws Exception {
        String blankTagJson = """
                {
                    "title": "Title",
                    "content": "This is valid content.",
                    "category": "Category",
                    "tags": ["test", " "]
                }
                """;
        ResultMatcher blankTagAssertion = jsonPath("$.['tags[1]']").value("A tag cannot be blank");

        executeAndVerify(post("/api/posts"), blankTagJson, status().isBadRequest(), blankTagAssertion);
        executeAndVerify(put("/api/posts/{id}", POST_ID), blankTagJson, status().isBadRequest(), blankTagAssertion);

        verifyNoInteractions(postService);
    }

    @Test
    void shouldReturnAllFieldErrorsWhenPayloadIsEmpty() throws Exception {
        String emptyPayloadJson = "{ }";
        ResultMatcher[] missingFieldsAssertions = {
                jsonPath("$.title").value("Title is required"),
                jsonPath("$.content").value("Content is required"),
                jsonPath("$.category").value("Category is required"),
                jsonPath("$.tags").value("At least one tag is required")
        };

        executeAndVerify(post("/api/posts"), emptyPayloadJson, status().isBadRequest(), missingFieldsAssertions);
        executeAndVerify(put("/api/posts/{id}", POST_ID), emptyPayloadJson, status().isBadRequest(),
                missingFieldsAssertions);

        verifyNoInteractions(postService);
    }
}
