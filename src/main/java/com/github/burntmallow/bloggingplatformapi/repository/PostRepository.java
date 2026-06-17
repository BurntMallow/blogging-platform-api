package com.github.burntmallow.bloggingplatformapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.burntmallow.bloggingplatformapi.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT p FROM Post p
            WHERE (:term IS NULL OR :term = '')
            OR (
                LOWER(p.title)    LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(p.content)  LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(p.category) LIKE LOWER(CONCAT('%', :term, '%'))
            )
            """)
    List<Post> searchByTerm(@Param("term") String term);
}
