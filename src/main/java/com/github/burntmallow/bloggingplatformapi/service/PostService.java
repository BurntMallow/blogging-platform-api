package com.github.burntmallow.bloggingplatformapi.service;

import org.springframework.stereotype.Service;

import com.github.burntmallow.bloggingplatformapi.entity.Post;
import com.github.burntmallow.bloggingplatformapi.repository.PostRepository;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post createPost(Post newPost) {
        return postRepository.save(newPost);
    }
}
