package com.github.burntmallow.bloggingplatformapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        assertEquals(2, retrievedPost.getTags().size(), "Post should have exactly 2 tags attached");

        Tag verifiedExistingTag = retrievedPost.getTags().stream()
                .filter(t -> t.getName().equals(OLD_TAG_NAME))
                .findFirst()
                .orElseThrow();

        Tag verifiedNewTag = retrievedPost.getTags().stream()
                .filter(t -> t.getName().equals(NEW_TAG_NAME))
                .findFirst()
                .orElseThrow();

        assertEquals(expectedExistingTagId, verifiedExistingTag.getId(),
                "The database must reuse the exact database ID for the existing tag row");
        assertNotNull(verifiedNewTag.getId(),
                "The database must generate a brand new primary key ID for the new tag");
        assertNotEquals(expectedExistingTagId, verifiedNewTag.getId(),
                "The new tag must have its own distinct primary key ID");
    }
}
