package com.pacgram.user.repository;

import com.pacgram.auth.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSearchRepository extends JpaRepository<UserAccount, Long> {
    @Query("""
            select u from UserAccount u
            where u.id <> :viewerId
              and not exists (
                  select b.id from UserBlock b
                  where (b.blocker.id = :viewerId and b.blocked.id = u.id)
                     or (b.blocker.id = u.id and b.blocked.id = :viewerId)
              )
              and (lower(u.username) like lower(concat('%', :query, '%'))
                   or lower(u.name) like lower(concat('%', :query, '%')))
            order by u.username asc
            """)
    Page<UserAccount> searchUsers(
            @Param("viewerId") Long viewerId, @Param("query") String query, Pageable pageable);
}
