package com.pacgram.post.repository;

import com.pacgram.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    long countByPostId(Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    void deleteByPostId(Long postId);
    void deleteByUserId(Long userId);

    @Modifying
    @Query("delete from PostLike l where l.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Query("select l.post.id, count(l.id) from PostLike l where l.post.id in :postIds group by l.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("select l.post.id from PostLike l where l.user.id = :userId and l.post.id in :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
