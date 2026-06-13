package com.github.burntmallow.bloggingplatformapi.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.burntmallow.bloggingplatformapi.dto.PostRequest;
import com.github.burntmallow.bloggingplatformapi.dto.PostResponse;
import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.repository.PostRepository;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostResponse createPost(PostRequest newPostRequest) {
        Post newPost = mapToNewPost(newPostRequest);
        Post savedPost = postRepository.save(newPost);
        return mapEntityToResponse(savedPost);
    }

    private Post mapToNewPost(PostRequest postRequest) {
        Post post = new Post();
        post.setTitle(postRequest.title());
        post.setContent(postRequest.content());
        post.setCategory(postRequest.category());
        post.getTags().addAll(postRequest.tags());
        return post;
    }

    private void updateExistingPost(PostRequest postRequest, Post existingPost) {
        existingPost.setTitle(postRequest.title());
        existingPost.setContent(postRequest.content());
        existingPost.setCategory(postRequest.category());
        existingPost.getTags().clear();
        existingPost.getTags().addAll(postRequest.tags());
    }

    private PostResponse mapEntityToResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getTags(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
