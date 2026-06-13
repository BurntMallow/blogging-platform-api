package com.github.burntmallow.bloggingplatformapi.controller;

import org.springframework.web.bind.annotation.RestController;

import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.service.PostService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post newPost) {
        Post savedPost = postService.createPost(newPost);
        return new ResponseEntity<>(savedPost, HttpStatus.CREATED);
    }
}
