package com.github.burntmallow.bloggingplatformapi.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.github.burntmallow.bloggingplatformapi.dto.PostRequest;
import com.github.burntmallow.bloggingplatformapi.dto.PostResponse;
import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.entity.Tag;
import com.github.burntmallow.bloggingplatformapi.repository.PostRepository;
import com.github.burntmallow.bloggingplatformapi.repository.TagRepository;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;

    public PostService(PostRepository postRepository, TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public PostResponse createPost(PostRequest postRequest) {
        Post newPost = mapToNewPost(postRequest);
        Post savedPost = postRepository.save(newPost);
        return mapEntityToResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(PostRequest postRequest, Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"));
        updateExistingPost(postRequest, post);
        return mapEntityToResponse(post);
    }

    @Transactional
    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found");
        }
        postRepository.deleteById(id);
    }

    @Transactional
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found"));
        return mapEntityToResponse(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getAllPostsContainingTerm(String term) {
        List<Post> posts;
        if (term == null || term.isBlank()) {
            posts = postRepository.findAll();
        } else {
            posts = postRepository.searchByTerm(term);
        }

        return posts.stream().map(this::mapEntityToResponse).toList();
    }

    private Post mapToNewPost(PostRequest postRequest) {
        Post post = new Post();
        post.setTitle(postRequest.title());
        post.setContent(postRequest.content());
        post.setCategory(postRequest.category());
        convertStringsToTag(postRequest.tags(), post);
        return post;
    }

    private void updateExistingPost(PostRequest postRequest, Post existingPost) {
        existingPost.setTitle(postRequest.title());
        existingPost.setContent(postRequest.content());
        existingPost.setCategory(postRequest.category());
        existingPost.clearTags();
        convertStringsToTag(postRequest.tags(), existingPost);
    }

    private PostResponse mapEntityToResponse(Post post) {
        Set<String> tagNames = post.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                tagNames,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private void convertStringsToTag(List<String> tagNames, Post post) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName).orElseGet(() -> tagRepository.save(new Tag(tagName)));
            post.addTag(tag);
        }
    }
}
