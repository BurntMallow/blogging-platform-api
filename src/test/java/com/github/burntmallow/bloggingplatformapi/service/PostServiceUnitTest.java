package com.github.burntmallow.bloggingplatformapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.github.burntmallow.bloggingplatformapi.dto.PostRequest;
import com.github.burntmallow.bloggingplatformapi.dto.PostResponse;
import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.entity.Tag;
import com.github.burntmallow.bloggingplatformapi.repository.PostRepository;
import com.github.burntmallow.bloggingplatformapi.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
public class PostServiceUnitTest {

    private static final String TITLE = "Title";
    private static final String CONTENT = "This is the content.";
    private static final String CATEGORY = "Category";
    private static final String OLD_TAG_NAME = "old";
    private static final String NEW_TAG_NAME = "new";
    private static final String OLD_TITLE = "Old Title";
    private static final String OLD_CONTENT = "This old content.";
    private static final String OLD_CATEGORY = "Old Category";
    private static final Long OLD_POST_ID = 2L;
    private static final Long POST_ID = 2L;
    private static final Long NEW_TAG_ID = 2L;

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void shouldCreatePostByReusingExistingTagsAndPersistingNewTags() {
        PostRequest request = createDefaultRequest();
        Tag existingTag = createExistingTag();

        when(tagRepository.findByName(OLD_TAG_NAME)).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByName(NEW_TAG_NAME)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag internalNewTag = invocation.getArgument(0);
            ReflectionTestUtils.setField(internalNewTag, "id", NEW_TAG_ID);
            return internalNewTag;
        });
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post internalNewPost = invocation.getArgument(0);

            assertThat(internalNewPost.getTags())
                    .as("Service failed to reuse the existing database tag instance.")
                    .contains(existingTag);

            assertThat(internalNewPost.getTags())
                    .filteredOn(tag -> tag.getName().equals(NEW_TAG_NAME))
                    .hasSize(1)
                    .element(0)
                    .extracting(Tag::getId)
                    .as("The new tag was not saved/managed before being attached to the post.")
                    .isEqualTo(NEW_TAG_ID);

            ReflectionTestUtils.setField(internalNewPost, "id", POST_ID);
            return internalNewPost;
        });

        PostResponse response = postService.createPost(request);

        assertThat(response)
                .as("The mapped PostResponse did not match the expected repository output values")
                .isEqualTo(createExpectedResponse());
    }

    @Test
    void shouldUpdatePostWhenRequestedPostExist() {
        Post existingPost = createExistingPost();
        PostRequest request = createDefaultRequest();
        Tag existingTag = createExistingTag();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(existingPost));
        when(tagRepository.findByName(OLD_TAG_NAME)).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByName(NEW_TAG_NAME)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(new Tag(NEW_TAG_NAME));

        PostResponse response = postService.updatePost(request, POST_ID);

        PostResponse expected = createExpectedResponse();
        assertThat(response)
                .as("The mapped PostResponse did not match the expected repository output values")
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenPostDoesNotExistOnUpdate() {
        PostRequest request = createDefaultRequest();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrowsNotFound(
                () -> postService.updatePost(request, POST_ID),
                "Blog post not found");
    }

    @Test
    void shouldDeletePostWhenRequestedPostExist() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        doNothing().when(postRepository).deleteById(POST_ID);

        assertThatCode(() -> postService.deletePost(POST_ID)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowNotFoundWhenPostDoesNotExistOnDelete() {
        when(postRepository.existsById(POST_ID)).thenReturn(false);

        assertThatThrowsNotFound(
                () -> postService.deletePost(POST_ID),
                "Blog post not found");
    }

    @Test
    void shouldGetPostWhenRequestedPostExist() {
        Post post = createDefaultPost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        PostResponse response = postService.getPost(POST_ID);

        PostResponse expected = createExpectedResponse();
        assertThat(response)
                .as("The mapped PostResponse did not match the expected repository output values")
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenPostDoesNotExistOnGet() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrowsNotFound(
                () -> postService.getPost(POST_ID),
                "Blog post not found");
    }

    @Test
    void shouldReturnAllPostsWhenTermIsNullAndPostsExist() {
        Post post1 = createExistingPost();
        Post post2 = createDefaultPost();
        when(postRepository.findAll()).thenReturn(List.of(post1, post2));

        List<PostResponse> response = postService.getAllPostsContainingTerm(null);

        List<PostResponse> expected = List.of(createExpectedResponseForExistingPost(), createExpectedResponse());
        assertThat(response)
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyListWhenTermIsNullAndNoPostsExist() {
        List<Post> emptyList = new ArrayList<>();
        when(postRepository.findAll()).thenReturn(emptyList);

        List<PostResponse> response = postService.getAllPostsContainingTerm(null);
        assertThat(response).isEmpty();
    }
    @Test
    void shouldReturnAllPostsWhenTermIsBlankAndPostsExist() {
        Post post1 = createExistingPost();
        Post post2 = createDefaultPost();
        when(postRepository.findAll()).thenReturn(List.of(post1, post2));

        List<PostResponse> response = postService.getAllPostsContainingTerm(" ");

        List<PostResponse> expected = List.of(createExpectedResponseForExistingPost(), createExpectedResponse());
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void shouldReturnMatchingPostsWhenTermMatchesRecords() {
        Post post = createExistingPost();
        when(postRepository.searchByTerm(anyString())).thenReturn(List.of(post));

        List<PostResponse> response = postService.getAllPostsContainingTerm("Old");

        List<PostResponse> expected = List.of(createExpectedResponseForExistingPost());
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void sshouldReturnEmptyListWhenTermMatchesNoRecords() {
        List<Post> emptyList = new ArrayList<>();
        when(postRepository.searchByTerm(anyString())).thenReturn(emptyList);

        List<PostResponse> response = postService.getAllPostsContainingTerm("nonexistent");
        assertThat(response).isEmpty();
    }


    private void assertThatThrowsNotFound(ThrowingCallable methodCall, String expectedMessage) {
        assertThatThrownBy(methodCall)
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND)
                .hasMessageContaining(expectedMessage);
    }

    // Matches createDefaultRequest and createExpectedResponse
    private Post createDefaultPost() {
        Post defaultPost = new Post(TITLE, CONTENT, CATEGORY);
        ReflectionTestUtils.setField(defaultPost, "id", POST_ID);
        defaultPost.addTag(new Tag(OLD_TAG_NAME));
        defaultPost.addTag(new Tag(NEW_TAG_NAME));
        return defaultPost;
    }

    // To be modified, matches createExpectedResponseForExistingPost
    private Post createExistingPost() {
        Post existingPost = new Post(OLD_TITLE, OLD_CONTENT, OLD_CATEGORY);
        ReflectionTestUtils.setField(existingPost, "id", OLD_POST_ID);
        existingPost.addTag(new Tag(OLD_TAG_NAME));
        return existingPost;
    }

    private Tag createExistingTag() {
        return new Tag(OLD_TAG_NAME);
    }

    private PostRequest createDefaultRequest() {
        return new PostRequest(TITLE, CONTENT, CATEGORY, List.of(OLD_TAG_NAME, NEW_TAG_NAME));
    }

    private PostResponse createExpectedResponse() {
        return new PostResponse(POST_ID, TITLE, CONTENT, CATEGORY, Set.of(OLD_TAG_NAME, NEW_TAG_NAME), null, null);
    }

    private PostResponse createExpectedResponseForExistingPost() {
        return new PostResponse(OLD_POST_ID, OLD_TITLE, OLD_CONTENT, OLD_CATEGORY, Set.of(OLD_TAG_NAME), null, null);
    }
}
