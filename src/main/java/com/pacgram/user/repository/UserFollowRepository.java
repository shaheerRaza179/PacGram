package com.pacgram.user.repository;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.user.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    long countByFollowedId(Long followedId);
    long countByFollowerId(Long followerId);
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);

    @Modifying
    @Query("delete from UserFollow f where f.follower.id = :userId or f.followed.id = :userId")
    int deleteByFollowerIdOrFollowedId(@Param("userId") Long userId);
    @Query("""
            select f.follower from UserFollow f
            where f.followed.id = :userId
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = f.follower.id)
                     or (b.blocker.id = f.follower.id and b.blocked.id = :viewerId)
              )
            order by f.createdAt desc
            """)
    Page<UserAccount> findFollowers(
            @Param("userId") Long userId, @Param("viewerId") Long viewerId, Pageable pageable);

    @Query("""
            select f.followed from UserFollow f
            where f.follower.id = :userId
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = f.followed.id)
                     or (b.blocker.id = f.followed.id and b.blocked.id = :viewerId)
              )
            order by f.createdAt desc
            """)
    Page<UserAccount> findFollowing(
            @Param("userId") Long userId, @Param("viewerId") Long viewerId, Pageable pageable);

    @Query("""
            select u from UserAccount u
            where u.id <> :viewerId
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = u.id)
                     or (b.blocker.id = u.id and b.blocked.id = :viewerId)
              )
              and not exists (
                  select f.id from UserFollow f
                  where f.follower.id = :viewerId and f.followed.id = u.id
              )
            order by u.createdAt desc
            """)
    Page<UserAccount> findSuggestions(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("select f.followed.id from UserFollow f " +
            "where f.follower.id = :followerId and f.followed.id in :followedIds")
    List<Long> findFollowedUserIds(
            @Param("followerId") Long followerId, @Param("followedIds") List<Long> followedIds);
}
