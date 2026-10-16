package com.pacgram.moderation.repository;

import com.pacgram.moderation.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    @Modifying
    @Query("delete from UserReport r where r.reporter.id = :userId or r.reportedUser.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
