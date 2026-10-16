package com.pacgram.post.repository;

import com.pacgram.post.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    @EntityGraph(attributePaths = {"post", "post.author"})
    @Query("""
            select s from SavedPost s
            where s.user.id = :viewerId
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = s.post.author.id)
                     or (b.blocker.id = s.post.author.id and b.blocked.id = :viewerId)
              )
              and (s.post.author.id = :viewerId
                   or s.post.author.privateAccount = false
                   or exists (
                       select f.id from UserFollow f
                       where f.follower.id = :viewerId and f.followed.id = s.post.author.id
                   ))
            order by s.createdAt desc
            """)
    Page<SavedPost> findVisibleByUser(@Param("viewerId") Long viewerId, Pageable pageable);
    void deleteByPostId(Long postId);
    void deleteByUserId(Long userId);

    @Modifying
    @Query("delete from SavedPost s where s.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Query("select s.post.id from SavedPost s where s.user.id = :userId and s.post.id in :postIds")
    List<Long> findSavedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
