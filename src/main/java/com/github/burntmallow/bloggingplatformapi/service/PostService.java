package com.github.burntmallow.bloggingplatformapi.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        convertStringsToTag(postRequest.tags(), post);
        return post;
    }

    private void updateExistingPost(PostRequest postRequest, Post existingPost) {
        existingPost.setTitle(postRequest.title());
        existingPost.setContent(postRequest.content());
        existingPost.setCategory(postRequest.category());
        existingPost.getTags().clear();
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
            Tag tag = tagRepository.findByName(tagName).orElseGet(() -> new Tag(tagName));
            post.getTags().add(tag);
        }
    }
}
