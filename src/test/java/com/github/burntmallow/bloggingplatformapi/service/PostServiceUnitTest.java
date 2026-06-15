package com.github.burntmallow.bloggingplatformapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.util.ReflectionTestUtils;

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
    private static final Long POST_ID = 1L;
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
        Tag existingTag = new Tag(OLD_TAG_NAME);

        when(tagRepository.findByName(OLD_TAG_NAME)).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByName(NEW_TAG_NAME)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag internalNewTag = invocation.getArgument(0);
            ReflectionTestUtils.setField(internalNewTag, "id", NEW_TAG_ID);
            return internalNewTag;
        });
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post internalNewPost = invocation.getArgument(0);

            assertTrue(internalNewPost.getTags().contains(existingTag),
                    "Service failed to reuse the existing database tag instance.");

            Tag createdTag = internalNewPost.getTags().stream()
                    .filter(tag -> tag.getName().equals(NEW_TAG_NAME))
                    .findFirst()
                    .orElseThrow(
                            () -> new AssertionError("The new tag '" + NEW_TAG_NAME + "' was not added to the post."));

            assertEquals(NEW_TAG_ID, createdTag.getId(),
                    "The new tag was not saved/managed before being attached to the post.");

            ReflectionTestUtils.setField(internalNewPost, "id", POST_ID);
            return internalNewPost;
        });

        PostResponse response = postService.createPost(request);

        assertEquals(createExpectedResponse(), response,
                "The mapped PostResponse did not match the expected repository output values");
    }

    private PostRequest createDefaultRequest() {
        return new PostRequest(TITLE, CONTENT, CATEGORY, List.of(OLD_TAG_NAME, NEW_TAG_NAME));
    }

    private PostResponse createExpectedResponse() {
        return new PostResponse(POST_ID, TITLE, CONTENT, CATEGORY, Set.of(OLD_TAG_NAME, NEW_TAG_NAME), null, null);
    }
}
