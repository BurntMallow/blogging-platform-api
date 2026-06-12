package com.github.burntmallow.bloggingplatformapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.burntmallow.bloggingplatformapi.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

}
