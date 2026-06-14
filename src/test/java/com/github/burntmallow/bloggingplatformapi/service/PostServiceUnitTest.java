package com.github.burntmallow.bloggingplatformapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private static final String TAG_NAME = "test";
    private static final Long POST_ID = 1L;

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void shouldSavePostWithNewTagWhenTagIsEmpty() {
        PostRequest request = createDefaultRequest();

        when(tagRepository.findByName(TAG_NAME)).thenReturn(Optional.empty());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post internalNewPost = invocation.getArgument(0);
            internalNewPost.setId(POST_ID);
            return internalNewPost;
        });

        PostResponse response = postService.createPost(request);

        assertEquals(createExpectedResponse(), response,
                "The mapped PostResponse did not match the expected repository output values");
    }

    @Test
    void shouldSavePostWithExistingTagWhenTagIsPresent() {
        PostRequest request = createDefaultRequest();
        Tag existingTag = new Tag(TAG_NAME);

        when(tagRepository.findByName(TAG_NAME)).thenReturn(Optional.of(existingTag));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post internalNewPost = invocation.getArgument(0);

            Tag linkedTag = internalNewPost.getTags().iterator().next();
            assertSame(existingTag, linkedTag, "Service failed to reuse the existing database tag instance.");

            internalNewPost.setId(POST_ID);
            return internalNewPost;
        });

        PostResponse response = postService.createPost(request);

        assertEquals(createExpectedResponse(), response,
                "The mapped PostResponse did not match the expected repository output values");
    }

    private PostRequest createDefaultRequest() {
        return new PostRequest(TITLE, CONTENT, CATEGORY, List.of(TAG_NAME));
    }

    private PostResponse createExpectedResponse() {
        return new PostResponse(POST_ID, TITLE, CONTENT, CATEGORY, Set.of(TAG_NAME), null, null);
    }
}
