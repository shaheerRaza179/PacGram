package com.pacgram.post.repository;

import com.pacgram.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    long countByPostId(Long postId);
    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
    void deleteByPostId(Long postId);

    @Modifying
    @Query("delete from Comment c where c.author.id = :authorId")
    int deleteByAuthorId(@Param("authorId") Long authorId);

    @Modifying
    @Query("delete from Comment c where c.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Query("select c.post.id, count(c.id) from Comment c where c.post.id in :postIds group by c.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
}
