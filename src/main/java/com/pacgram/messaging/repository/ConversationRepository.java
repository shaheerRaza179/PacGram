package com.pacgram.messaging.repository;

import com.pacgram.messaging.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = {"userOne", "userTwo"})
    @Query("""
            select c from Conversation c
            where (c.userOne.id = :userId or c.userTwo.id = :userId)
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :userId
                         and (b.blocked.id = c.userOne.id or b.blocked.id = c.userTwo.id))
                     or (b.blocked.id = :userId
                         and (b.blocker.id = c.userOne.id or b.blocker.id = c.userTwo.id))
              )
            order by c.lastMessageAt desc
            """)
    Page<Conversation> findForUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"userOne", "userTwo"})
    Optional<Conversation> findByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);

    @EntityGraph(attributePaths = {"userOne", "userTwo"})
    Optional<Conversation> findById(Long id);

    @Query("select c.id from Conversation c where c.userOne.id = :userId or c.userTwo.id = :userId")
    List<Long> findIdsForParticipant(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Conversation c where c.id in :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            select case when count(c) > 0 then true else false end
            from Conversation c
            where c.id = :conversationId and (c.userOne.id = :userId or c.userTwo.id = :userId)
            """)
    boolean isParticipant(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
