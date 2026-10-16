package com.pacgram.post.repository;

import com.pacgram.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    long countByAuthorId(Long authorId);
    List<Post> findAllByAuthorId(Long authorId);

    @EntityGraph(attributePaths = "author")
    @Query("select p from Post p where p.author.id = :authorId order by p.createdAt desc")
    Page<Post> findByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p
            where (p.author.id = :viewerId or exists (
                select f.id from UserFollow f
                where f.follower.id = :viewerId and f.followed.id = p.author.id
            ))
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = p.author.id)
                     or (b.blocker.id = p.author.id and b.blocked.id = :viewerId)
              )
            order by p.createdAt desc
            """)
    Page<Post> findFeed(@Param("viewerId") Long viewerId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p
            where p.author.id <> :viewerId
              and p.author.privateAccount = false
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = p.author.id)
                     or (b.blocker.id = p.author.id and b.blocked.id = :viewerId)
              )
              and not exists (
                  select f.id from UserFollow f
                  where f.follower.id = :viewerId and f.followed.id = p.author.id
              )
            order by p.createdAt desc
            """)
    Page<Post> findExplore(@Param("viewerId") Long viewerId, Pageable pageable);
}
