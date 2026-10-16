package com.pacgram.messaging.repository;

import com.pacgram.messaging.entity.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    @EntityGraph(attributePaths = {"sender", "conversation"})
    Page<DirectMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "conversation"})
    @Query("""
            select m from DirectMessage m
            where m.conversation.id in :conversationIds
              and m.id = (
                  select max(latest.id) from DirectMessage latest
                  where latest.conversation.id = m.conversation.id
              )
            """)
    List<DirectMessage> findLastMessages(@Param("conversationIds") List<Long> conversationIds);

    @Query("""
            select m.conversation.id, count(m.id) from DirectMessage m
            where m.conversation.id in :conversationIds
              and m.sender.id <> :viewerId and m.readAt is null
            group by m.conversation.id
            """)
    List<Object[]> countUnreadByConversation(
            @Param("conversationIds") List<Long> conversationIds, @Param("viewerId") Long viewerId);

    @Modifying
    @Query("""
            update DirectMessage m set m.readAt = :readAt
            where m.conversation.id = :conversationId
              and m.sender.id <> :readerId and m.readAt is null
            """)
    int markIncomingRead(
            @Param("conversationId") Long conversationId,
            @Param("readerId") Long readerId,
            @Param("readAt") Instant readAt);

    void deleteByConversationId(Long conversationId);

    @Modifying
    @Query("delete from DirectMessage m where m.conversation.id in :conversationIds")
    int deleteByConversationIdIn(@Param("conversationIds") List<Long> conversationIds);
}
