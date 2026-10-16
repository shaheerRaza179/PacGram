package com.pacgram.user.repository;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.user.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
            select case when count(b) > 0 then true else false end
            from UserBlock b
            where (b.blocker.id = :firstId and b.blocked.id = :secondId)
               or (b.blocker.id = :secondId and b.blocked.id = :firstId)
            """)
    boolean existsBlockBetween(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Modifying
    @Query("delete from UserBlock b where b.blocker.id = :userId or b.blocked.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Query("select b.blocked from UserBlock b where b.blocker.id = :userId order by b.createdAt desc")
    Page<UserAccount> findBlockedUsers(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select b.blocked.id from UserBlock b
            where b.blocker.id = :userId or b.blocked.id = :userId
            """)
    List<Long> findConnectedUserIds(@Param("userId") Long userId);

    @Query("""
            select b.blocked.id from UserBlock b
            where b.blocker.id = :userId and b.blocked.id in :targetIds
            """)
    List<Long> findBlockedUserIds(
            @Param("userId") Long userId, @Param("targetIds") List<Long> targetIds);
}
