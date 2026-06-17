package com.github.burntmallow.bloggingplatformapi.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    // ==========================================
    // SUCCESS PATH TESTS
    // ==========================================

    @Test
    void shouldReturn201AndPostDetailsOnSuccessfulCreate() throws Exception {
        when(postService.createPost(any(PostRequest.class))).thenReturn(createExpectedResponse());

        executeAndVerify(post("/api/posts"), validRequestJson, status().isCreated(), getPostDetailsAssertions());
        verify(postService).createPost(any(PostRequest.class));
    }

    @Test
    void shouldReturn200AndPostDetailsOnSuccessfulUpdate() throws Exception {
        when(postService.updatePost(any(PostRequest.class), eq(POST_ID))).thenReturn(createExpectedResponse());

        executeAndVerify(put("/api/posts/{id}", POST_ID), validRequestJson, status().isOk(),
                getPostDetailsAssertions());
        verify(postService).updatePost(any(PostRequest.class), eq(POST_ID));
    }

    @Test
    void shouldReturn200AndPostDetailsOnSuccessfulGet() throws Exception {
        when(postService.getPost(eq(POST_ID))).thenReturn(createExpectedResponse());

        executeAndVerify(get("/api/posts/{id}", POST_ID), null, status().isOk(), getPostDetailsAssertions());
        verify(postService).getPost(eq(POST_ID));
    }

    @Test
    void shouldReturn204OnSuccessfulDelete() throws Exception {
        doNothing().when(postService).deletePost(eq(POST_ID));

        executeAndVerify(delete("/api/posts/{id}", POST_ID), null, status().isNoContent());
        verify(postService).deletePost(eq(POST_ID));
    }

    // ==========================================
    // 404 NOT FOUND ERROR TESTS
    // ==========================================

    @Test
    void shouldReturn404WhenGetResourceNotFound() throws Exception {
        when(postService.getPost(eq(POST_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"));

        executeAndVerify(get("/api/posts/{id}", POST_ID), null, status().isNotFound(), getNotFoundAssertion());
        verify(postService).getPost(eq(POST_ID));
    }

    @Test
    void shouldReturn404WhenUpdateResourceNotFound() throws Exception {
        when(postService.updatePost(any(PostRequest.class), eq(POST_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"));

        executeAndVerify(put("/api/posts/{id}", POST_ID), validRequestJson, status().isNotFound(),
                getNotFoundAssertion());
        verify(postService).updatePost(any(PostRequest.class), eq(POST_ID));
    }

    @Test
    void shouldReturn404WhenDeleteResourceNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"))
                .when(postService).deletePost(eq(POST_ID));

        executeAndVerify(delete("/api/posts/{id}", POST_ID), null, status().isNotFound(), getNotFoundAssertion());
        verify(postService).deletePost(eq(POST_ID));
    }

    // ==========================================
    // VALIDATION ERROR TESTS
    // ==========================================

    @Test
    void shouldReturnSpecificErrorWhenCreatePayloadHasBlankTagIndex() throws Exception {
        String blankTagJson = createBlankTagJson();

        executeAndVerify(post("/api/posts"), blankTagJson, status().isBadRequest(), getBlankTagAssertion());
        verifyNoInteractions(postService);
    }

    @Test
    void shouldReturnSpecificErrorWhenUpdatePayloadHasBlankTagIndex() throws Exception {
        String blankTagJson = createBlankTagJson();

        executeAndVerify(put("/api/posts/{id}", POST_ID), blankTagJson, status().isBadRequest(),
                getBlankTagAssertion());
        verifyNoInteractions(postService);
    }

    @Test
    void shouldReturnAllFieldErrorsWhenCreatePayloadIsEmpty() throws Exception {
        executeAndVerify(post("/api/posts"), "{ }", status().isBadRequest(), getMissingFieldsAssertions());
        verifyNoInteractions(postService);
    }

    @Test
    void shouldReturnAllFieldErrorsWhenUpdatePayloadIsEmpty() throws Exception {
        executeAndVerify(put("/api/posts/{id}", POST_ID), "{ }", status().isBadRequest(), getMissingFieldsAssertions());
        verifyNoInteractions(postService);
    }

    // ==========================================
    // REUSABLE REFACTORED ASSERTIONS & HELPERS
    // ==========================================

    private ResultMatcher[] getPostDetailsAssertions() {
        return new ResultMatcher[] {
                jsonPath("$.id").value(POST_ID),
                jsonPath("$.title").value(TITLE),
                jsonPath("$.content").value(CONTENT),
                jsonPath("$.category").value(CATEGORY),
                jsonPath("$.tags").isArray(),
                jsonPath("$.tags").value(containsInAnyOrder(OLD_TAG_NAME, NEW_TAG_NAME))
        };
    }

    private ResultMatcher getNotFoundAssertion() {
        return jsonPath("$.error").value("Blog post not found");
    }

    private ResultMatcher getBlankTagAssertion() {
        return jsonPath("$.['tags[1]']").value("A tag cannot be blank");
    }

    private ResultMatcher[] getMissingFieldsAssertions() {
        return new ResultMatcher[] {
                jsonPath("$.title").value("Title is required"),
                jsonPath("$.content").value("Content is required"),
                jsonPath("$.category").value("Category is required"),
                jsonPath("$.tags").value("At least one tag is required")
        };
    }

    private String createBlankTagJson() {
        return """
                {
                    "title": "Title",
                    "content": "This is valid content.",
                    "category": "Category",
                    "tags": ["test", " "]
                }
                """;
    }

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

        if (expectedJsonPaths != null) {
            for (ResultMatcher pathMatcher : expectedJsonPaths) {
                responseActions.andExpect(pathMatcher);
            }
        }
    }
}