package com.github.burntmallow.bloggingplatformapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.entity.Tag;
import com.github.burntmallow.bloggingplatformapi.repository.PostRepository;
import com.github.burntmallow.bloggingplatformapi.repository.TagRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
        Long postId = populatePostAndReturnId(TITLE, CONTENT, CATEGORY, OLD_TAG_NAME, NEW_TAG_NAME);

        Post postToUpdate = postRepository.findById(postId).orElseThrow();
        postToUpdate.setTitle("My Updated Blog Post");
        postToUpdate.setContent("This is the updated content of my first blog post.");

        postRepository.saveAndFlush(postToUpdate);
        entityManager.clear();

        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertThat(updatedPost.getTitle()).isEqualTo("My Updated Blog Post");
        assertThat(updatedPost.getContent()).isEqualTo("This is the updated content of my first blog post.");
        assertThat(updatedPost.getCategory()).isEqualTo("Category");
    }

    @Test
    void shouldDeletePostWhenIdExist() {
        Long postId = populatePostAndReturnId(TITLE, CONTENT, CATEGORY, OLD_TAG_NAME, NEW_TAG_NAME);

        postRepository.deleteById(postId);
        postRepository.flush();

        assertThat(postRepository.findById(postId)).isEmpty();
    }

    @Test
    void shouldReturnEmptyOptionalWhenPostDoesNotExist() {
        Optional<Post> result = postRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindPostByIdWhenIdExists() {
        Long postId = populatePostAndReturnId(TITLE, CONTENT, CATEGORY, OLD_TAG_NAME);

        Optional<Post> result = postRepository.findById(postId);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo(TITLE);
        assertThat(result.get().getContent()).isEqualTo(CONTENT);
    }

    @Test
    void shouldReturnAllPosts() {
        Long postId1 = populatePostAndReturnId("First Title", "This is a content", CATEGORY, OLD_TAG_NAME);
        Long postId2 = populatePostAndReturnId("Second Title", "This is a content too", CATEGORY, NEW_TAG_NAME);

        List<Post> posts = postRepository.findAll();

        assertThat(posts)
                .hasSize(2)
                .extracting(Post::getId)
                .containsExactlyInAnyOrder(postId1, postId2);
    }

    @Test
    void shouldReturnPostsContainingTerm() {
        Long postId1 = populatePostAndReturnId("First Title", "This is a content", CATEGORY, OLD_TAG_NAME);
        Long postId2 = populatePostAndReturnId("Second Title", "This is a content too", CATEGORY, NEW_TAG_NAME);

        List<Post> postsContainingCategory = postRepository.searchByTerm(CATEGORY);

        assertThat(postsContainingCategory)
                .hasSize(2)
                .extracting(Post::getId)
                .containsExactlyInAnyOrder(postId1, postId2);

        entityManager.clear();

        List<Post> postsContainingFirst = postRepository.searchByTerm("first");

        assertThat(postsContainingFirst)
                .hasSize(1)
                .extracting(Post::getId)
                .containsExactlyInAnyOrder(postId1);

        entityManager.clear();

        List<Post> postsContainingToo = postRepository.searchByTerm("too");

        assertThat(postsContainingToo)
                .hasSize(1)
                .extracting(Post::getId)
                .containsExactlyInAnyOrder(postId2);
    }

    @Test
    void shouldReturnAllPostsWhenTermIsNullOrEmpty() {
        Long postId1 = populatePostAndReturnId("First Title", "This is a content", CATEGORY, OLD_TAG_NAME);
        Long postId2 = populatePostAndReturnId("Second Title", "This is a content too", CATEGORY, NEW_TAG_NAME);

        List<Post> postsWithNull = postRepository.searchByTerm(null);
        assertThat(postsWithNull)
                .hasSize(2)
                .extracting(Post::getId)
                .containsExactlyInAnyOrder(postId1, postId2);

        entityManager.clear();

        List<Post> postsWithEmpty = postRepository.searchByTerm("");
        assertThat(postsWithEmpty)
                .hasSize(2)
                .extracting(Post::getId)
                .containsExactlyInAnyOrder(postId1, postId2);
    }

    Long populatePostAndReturnId(String title, String content, String category, String... tags) {
        Post originalPost = new Post(title, content, category);
        for (String tag : tags) {
            Tag savedTag = tagRepository.save(new Tag(tag));
            originalPost.addTag(savedTag);
        }
        Post savedPost = postRepository.saveAndFlush(originalPost);
        Long postId = savedPost.getId();

        entityManager.clear();

        return postId;
    }
}
