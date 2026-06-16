package com.github.burntmallow.bloggingplatformapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.entity.Tag;
import com.github.burntmallow.bloggingplatformapi.repository.PostRepository;
import com.github.burntmallow.bloggingplatformapi.repository.TagRepository;

@DataJpaTest
public class PostRepositoryIntegrationTest {

    private static final String TITLE = "Title";
    private static final String CONTENT = "This is the content.";
    private static final String CATEGORY = "Category";
    private static final String OLD_TAG_NAME = "old";
    private static final String NEW_TAG_NAME = "new";

    @Autowired
    PostRepository postRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldPersistPostWithAppropriateTag() {
        Tag existingTag = new Tag(OLD_TAG_NAME);
        Tag savedExistingTag = tagRepository.saveAndFlush(existingTag);
        Long expectedExistingTagId = savedExistingTag.getId();

        Tag newTag = new Tag(NEW_TAG_NAME);
        Post post = new Post(TITLE, CONTENT, CATEGORY);
        post.addTag(savedExistingTag);
        Tag savedNewTag = tagRepository.save(newTag);
        post.addTag(savedNewTag);

        Post savedPost = postRepository.saveAndFlush(post);
        Long savedPostId = savedPost.getId();

        entityManager.clear();

        Post retrievedPost = postRepository.findById(savedPostId)
                .orElseThrow(() -> new AssertionError("Post was not saved to database"));

        assertThat(retrievedPost.getTags())
                .hasSize(2)
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder(OLD_TAG_NAME, NEW_TAG_NAME);

        Tag verifiedExistingTag = retrievedPost.getTags().stream()
                .filter(t -> t.getName().equals(OLD_TAG_NAME))
                .findFirst()
                .orElseThrow();

        Tag verifiedNewTag = retrievedPost.getTags().stream()
                .filter(t -> t.getName().equals(NEW_TAG_NAME))
                .findFirst()
                .orElseThrow();

        assertThat(verifiedExistingTag.getId()).isEqualTo(expectedExistingTagId);
        assertThat(verifiedNewTag.getId())
                .isNotNull()
                .isNotEqualTo(expectedExistingTagId);
    }

    @Test
    void shouldUpdateAllMutableFieldsOnPutLifecycle() {
        Post originalPost = new Post("Original Title", "Original Content", "Technology");
        Post savedPost = postRepository.saveAndFlush(originalPost);
        Long postId = savedPost.getId();

        entityManager.clear();

        Post postToUpdate = postRepository.findById(postId).orElseThrow();
        postToUpdate.setTitle("My Updated Blog Post");
        postToUpdate.setContent("This is the updated content of my first blog post.");

        postRepository.saveAndFlush(postToUpdate);
        entityManager.clear();

        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertThat(updatedPost.getTitle()).isEqualTo("My Updated Blog Post");
        assertThat(updatedPost.getContent()).isEqualTo("This is the updated content of my first blog post.");
        assertThat(updatedPost.getCategory()).isEqualTo("Technology");
    }
}
