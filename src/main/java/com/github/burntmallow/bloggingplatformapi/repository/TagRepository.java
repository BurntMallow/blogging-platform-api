package com.github.burntmallow.bloggingplatformapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.burntmallow.bloggingplatformapi.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
